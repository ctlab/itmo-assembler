package ru.ifmo.genetics.tools.scaffolder;
public class ScafEdge {
	public Scaffold x;
	public Scaffold y;
	public int ex;
	public int ey;
	public Edge edge;
	ScafEdge r;

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof ScafEdge)) {
			return false;
		}
		ScafEdge se = (ScafEdge) o;
		return x.equals(se.x) && y.equals(se.y) && ex == se.ex && ey == se.ey;
	}

	ScafEdge rev() {
		if (r != null) {
			return r;
		}
		r = new ScafEdge();
		r.y = x;
		r.x = y;
		r.r = this;
		r.edge = edge.rev();
		r.ey = ex;
		r.ex = ey;
		return r;
	}
}
