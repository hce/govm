package net.sourceforge.adela.tests.network;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import net.sourceforge.adela.exceptions.FunctionNotFoundException;
import net.sourceforge.adela.exceptions.WrongParameterCountException;
import net.sourceforge.adela.interfaces.IFunctionCallback;
import net.sourceforge.adela.interpreter.ADELAInterpreter;
import net.sourceforge.adela.interpreter.ADELAParseTree;

/**
 * @author hc
 * 
 */
public class SocketHandler extends Thread implements IFunctionCallback {
	/**
	 * Our connection
	 */
	private Socket connection;

	/**
	 * The socket's input stream
	 */
	private InputStream in;

	/**
	 * The socket's output stream
	 */
	private OutputStream out;

	/**
	 * This thread's stopped
	 */
	private boolean stopped = false;

	/**
	 * Create instance
	 * 
	 * @param connection
	 *            to use for communication
	 * @throws IOException
	 */
	public SocketHandler(Socket connection) throws IOException {
		this.connection = connection;
		this.in = connection.getInputStream();
		this.out = connection.getOutputStream();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		ADELAParseTree tree;
		ADELAInterpreter interpreter = new ADELAInterpreter();
		Map<String, Object> locals = new HashMap<String, Object>();

		interpreter.AddBuiltinHandlers();
		interpreter.AddMathHandlers();

		interpreter.AllowFullAccess("String", String.class);

		interpreter.DeclareClassSafe(BufferedReader.class);
		interpreter.DeclareClassSafe(BufferedWriter.class);
		interpreter.DeclareClassSafe(byte[].class);
		interpreter.SetVariable("out", new BufferedWriter(
				new OutputStreamWriter(out)));

		interpreter.SetCallback(this);
		while (!stopped) {
			try {

				String line = "";
				do {
					try {
						line = reader.readLine();
					} catch (Exception e) {
						stopped = true;
					}
				} while (line.trim().length() == 0);
				tree = ADELAParseTree.Parse(line);
				Object res = tree.doEvaluate(locals, interpreter);
				if (res != null) {
					out.write(res.toString().getBytes());
					out.write("\n".getBytes());
				}

			} catch (Exception e) {
				try {
					out.write(e.getMessage().getBytes());
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}
		try {
			connection.close();
			stopped = true;
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public Object FunctionCallback(String funcName, Object[] vParams)
			throws WrongParameterCountException, FunctionNotFoundException {
		SocketServerFunctions f;
		try {
			f = SocketServerFunctions.valueOf(funcName);
		} catch (Exception e) {
			throw new FunctionNotFoundException(funcName);
		}
		switch (f) {
		case log:
			try {
				out.write(vParams[0].toString().getBytes());
				out.write("\n".getBytes());
			} catch (IOException e) {
				e.printStackTrace();
			}
			break;
			
		case buffer:
			return new byte[(Integer) vParams[0]];

		case quit:
			try {
				out.write("Good bye!\n".getBytes());
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				connection.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.stopped = true;
			break;
		}

		return null;
	}
}
