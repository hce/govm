package net.sourceforge.adela.tests;

import java.util.HashMap;

public class MonitoringHashMap<T1, T2> extends HashMap<T1, T2> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private MapMonitor<T1, T2> monitor;

	public MonitoringHashMap(MapMonitor<T1, T2> monitor) {
		super();
		if (monitor == null) {
			this.monitor = new DummyMonitor();
		} else {
			this.monitor = monitor;
		}
	}

	@Override
	public T2 put(T1 key, T2 value) {
		if (!monitor.prePutEvent(key, value)) {
			return null;
		}
		T2 retval = super.put(key, value);
		monitor.putEvent(key, value);
		return retval;
	}

	public MapMonitor<T1, T2> getMonitor() {
		return monitor;
	}

	public void setMonitor(MapMonitor<T1, T2> monitor) {
		if (monitor == null) {
			this.monitor = new DummyMonitor();
		} else {
			this.monitor = monitor;
		}
	}

	public MonitoringHashMap() {
		super();
	}

	public class DummyMonitor implements MapMonitor<T1, T2> {
		public boolean prePutEvent(T1 key, T2 value) {
			return true;
		}

		public void putEvent(T1 key, T2 value) {
		}
	}
}
