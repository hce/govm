package net.sourceforge.adela.govm;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Map;

import net.sourceforge.adela.exceptions.ADELASyntaxException;
import net.sourceforge.adela.interpreter.ADELACompilerException;
import net.sourceforge.adela.interpreter.ADELAFunctionBlock;
import net.sourceforge.adela.interpreter.ADELAParser;
import net.sourceforge.adela.tests.GOVMCodeGenerator;

public class GOVMCompiler implements Runnable {
	private static final int MAX_BYTES = 1 << 20;
	private Socket s;
	private PrintStream logStream;

	public GOVMCompiler(PrintStream logStream) {
		this.logStream = logStream;
	}

	public GOVMCompiler(Socket s) {
		this.s = s;
	}

	public void compileFunction(String s, Map<String, ADELAFunctionBlock> res,
			GOVMCodeGenerator gcc) throws ADELACompilerException,
			ADELASyntaxException {
		logStream.println("===== Compiling " + s + " =====");
		ADELAFunctionBlock afb = res.get(s);
		gcc.setLabel("__FUNCTION__" + s);
		afb.generateGOVMCode(gcc);
	}

	public byte[] compile(byte[] input, PrintStream logStream) {
		ADELAParser ap = new ADELAParser();
		String str;
		try {
			str = new String(input, "utf-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace(logStream);
			return null;
		}
		Map<String, ADELAFunctionBlock> res;
		try {
			res = ap.BuildAutomat(str);
		} catch (ADELASyntaxException e) {
			e.printStackTrace(logStream);
			return null;
		}
		GOVMCodeGenerator gcc = new GOVMCodeGenerator(logStream);
		if (res.get("main") == null) {
			throw new ADELACompilerException("Entry point 'main' not found");
		}
		try {
			compileFunction("main", res, gcc);
		} catch (Exception e) {
			e.printStackTrace(logStream);
			return null;
		}
		res.remove("main");
		for (String s : res.keySet()) {
			try {
				compileFunction(s, res, gcc);
			} catch (Exception e) {
				e.printStackTrace(logStream);
				return null;
			}
		}
		byte[] cs = gcc.generate();
		logStream.println("===== COMPILATION SUCCESSFUL =====");
		return cs;
	}

	private static void usage() {
		System.err
				.println("USAGE: $(PROGRAM) [-s] [source.adela kompilat.govm]");
	}

	/**
	 * @param args
	 * @throws IOException
	 * @throws ADELASyntaxException
	 * @throws ADELACompilerException
	 */
	public static void main(String[] args) throws IOException,
			ADELASyntaxException, ADELACompilerException {
		if ((args.length == 0) || ((args.length == 1) && "-s".equals(args[0]))) {
			server();
			return;
		}
		if (args.length != 2) {
			usage();
			return;
		}
		compile(args[0], args[1]);
	}

	private static void compile(String source, String output)
			throws IOException {
		GOVMCompiler compiler = new GOVMCompiler(System.out);
		File f = new File(source);
		if (!f.exists()) {
			System.err.println("Input file does not exist!");
		}
		byte[] src = new byte[(int) f.length()];
		FileInputStream fis = new FileInputStream(f);
		fis.read(src);
		fis.close();
		byte[] bytecode = compiler.compile(src, System.out);
		FileOutputStream fos = new FileOutputStream(output);
		fos.write(bytecode);
		fos.close();
	}

	private static void server() {
		System.out.println("adl2govm compiler. Running as network server.");
		System.out.println("To compile a single script, use:");
		System.out.println("    java -jar adl2govm.jar SOURCE OUTPUT");
		ServerSocket ss;
		try {
			ss = new ServerSocket(2318);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		System.out.println("Listening");
		while (true) {
			Socket s;
			try {
				s = ss.accept();
			} catch (IOException e) {
				e.printStackTrace();
				continue;
			}
			System.out.println("Accepted a connection from "
					+ s.getRemoteSocketAddress().toString());
			GOVMCompiler gc = new GOVMCompiler(s);
			Thread t = new Thread(gc);
			t.start();
		}

	}

	public void run() {
		DataOutputStream dos = null;

		try {
			if (s == null) {
				throw new IllegalStateException(
						"Run can only be called with an associated socket");
			}
			s.setSoTimeout(10000);
			InputStream is = s.getInputStream();
			OutputStream os = s.getOutputStream();
			dos = new DataOutputStream(os);
			dos.writeBytes("GoVM");
			byte[] tmpbuf = new byte[4];
			is.read(tmpbuf);
			DataInputStream dis = new DataInputStream(new ByteArrayInputStream(
					tmpbuf));
			int bytes = dis.readInt();
			if ((bytes <= 0) || (bytes > MAX_BYTES)) {
				dos.writeInt(-1);
				return;
			}
			byte[] buf = new byte[bytes];
			is.read(buf);
			dos.writeInt(1);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			PrintStream logStream = new PrintStream(baos);
			this.logStream = logStream;
			byte[] bytecode = compile(buf, logStream);
			logStream.close();
			if (bytecode == null) {
				dos.writeInt(-2);
			} else {
				dos.writeInt(2);
				dos.writeInt(bytecode.length);
				dos.write(bytecode);
			}
			byte[] errors = baos.toByteArray();
			dos.writeInt(errors.length);
			dos.write(errors);
		} catch (SocketTimeoutException ste) {
			System.out.println("Socket timeout: "
					+ s.getRemoteSocketAddress().toString());
		} catch (Throwable e) {
			if (dos != null) {
				try {
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					PrintStream logStream = new PrintStream(baos);
					e.printStackTrace(logStream);
					logStream.close();
					dos.writeInt(-2);
					byte[] errors = baos.toByteArray();
					dos.writeInt(errors.length);
					dos.write(errors);
				} catch (IOException e1) {
					e1.printStackTrace();
					e.printStackTrace();
				}
			}
		} finally {
			try {
				s.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
