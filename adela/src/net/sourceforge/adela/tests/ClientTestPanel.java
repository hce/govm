package net.sourceforge.adela.tests;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.TextArea;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.JTextField;

import net.sourceforge.adela.exceptions.FunctionNotFoundException;
import net.sourceforge.adela.exceptions.WrongParameterCountException;
import net.sourceforge.adela.interfaces.IFunctionCallback;
import net.sourceforge.adela.interpreter.ADELAInterpreter;
import net.sourceforge.adela.interpreter.ADELAParseTree;

public class ClientTestPanel extends JPanel implements
		MapMonitor<String, Object>, IFunctionCallback {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7623115944529240600L;

	protected JTextField commandField;

	private TextArea outputArea;

	protected Map<String, Object> locals = new MonitoringHashMap<String, Object>(
			this);

	protected ADELAInterpreter environment;

	public ClientTestPanel() {
		initGUIComponents();
		setPreferredSize(new Dimension(640, 480));
		this.environment = new ADELAInterpreter();
		this.environment.setTrusted(true);
		this.environment.setAllowArbitraryInstanciiation(true);
		this.environment.SetCallback(this);
	}

	private void initGUIComponents() {
		this.commandField = new JTextField();
		this.commandField
				.addKeyListener(new CustomKeyListener<ClientTestPanel>(this) {
					@Override
					public void handleKeyPressed(ClientTestPanel param,
							KeyEvent e) {
						char keyChar = e.getKeyChar();
						switch (keyChar) {
						case '\n':
							try {
								ADELAParseTree apt = ADELAParseTree
										.Parse(param.commandField.getText());
								String foo = param.commandField.getText();
								Object res = apt.doEvaluate(param.locals,
										param.environment);
								if (res == null) {
								} else {
									log(res.toString());
								}
								if (param.commandField.getText().equals(foo)) {
									// empty field if it was not modified by the
									// script
									param.commandField.setText("");
								}
							} catch (Throwable e1) {
								log(e1.toString());
							}
							break;
						}
					}

					@Override
					public void handleKeyReleased(ClientTestPanel param,
							KeyEvent e) {
					}

					@Override
					public void handleKeyTyped(ClientTestPanel param, KeyEvent e) {
					}
				});
		this.outputArea = new TextArea();
		this.outputArea.setEditable(false);

		setLayout(new GridBagLayout());

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		add(this.outputArea, gbc);

		gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.SOUTH;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		gbc.gridy = 1;
		add(this.commandField, gbc);
	}

	protected void log(String s) {
		this.outputArea.append(s + "\n");
	}

	protected abstract class CustomKeyListener<T> implements KeyListener {
		private T param;

		public CustomKeyListener(T param) {
			this.param = param;
		}

		public void keyPressed(KeyEvent e) {
			handleKeyPressed(this.param, e);
		}

		public void keyReleased(KeyEvent e) {
			handleKeyReleased(this.param, e);
		}

		public void keyTyped(KeyEvent e) {
			handleKeyTyped(this.param, e);
		}

		public abstract void handleKeyPressed(T param, KeyEvent e);

		public abstract void handleKeyReleased(T param, KeyEvent e);

		public abstract void handleKeyTyped(T param, KeyEvent e);
	}

	public boolean prePutEvent(String key, Object value) {
		return true;
	}

	public void putEvent(String key, Object value) {
		log(key + " = " + ((value == null) ? "null" : value.toString()));
	}

	public Object FunctionCallback(String funcName, Object[] vParams)
			throws WrongParameterCountException, FunctionNotFoundException {
		try {
			Functions function = Functions.valueOf(funcName);
			switch (function) {
			case loadClass:
				if (vParams.length != 1) {
					throw new WrongParameterCountException(funcName,
							vParams.length, 1);
				}
				return Class.forName((String) vParams[0]);
			}
			throw new FunctionNotFoundException(funcName);
		} catch (Throwable t) {
			if (t instanceof WrongParameterCountException) {
				throw (WrongParameterCountException) t;
			} else if (t instanceof FunctionNotFoundException) {
				throw (FunctionNotFoundException) t;
			} else {
				throw new RuntimeException(t);
			}
		}
	}

	public enum Functions {
		loadClass
	}
}
