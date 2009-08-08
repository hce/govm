package net.sourceforge.adela.tests;

public interface MapMonitor<T1, T2> {
	public void putEvent(T1 key, T2 value);

	public boolean prePutEvent(T1 key, T2 value);
}
