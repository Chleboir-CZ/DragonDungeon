package net.trdlo.zelda.notiles;

public class Line {

	protected Point A, B;
	protected double a, b, c;

	/**
	 * Prázdný konstruktor používaný jen místními statickými továrními metodami
	 */
	protected Line() {
	}

	public static Line constructFromTwoPoints(Point A, Point B) {
		Line l = new Line();
		l.A = A;
		l.B = B;
		l.refreshCoefs();
		return l;
	}

	public static Line constructFromPointAndNormal(Point A, double a, double b) {
		Line l = new Line();
		l.a = a;
		l.b = b;
		l.A = A;
		l.B = new Point(A.x - b, A.y + a);
		l.c = -a * A.x - b * A.y;
		return l;
	}

	public static Line constructFromPointAndVector(Point A, double a, double b) {
		Line l = new Line();
		l.a = -b;
		l.b = a;
		l.A = A;
		l.B = new Point(A.x + a, A.y + b);
		l.c = b * A.x - a * A.y;
		return l;
	}

	public final void refreshCoefs() {
		a = A.y - B.y;
		b = B.x - A.x;
		c = -a * A.x - b * A.y;
	}

	public Point getA() {
		return A;
	}

	public Point getB() {
		return B;
	}

	public void setA(Point A) {
		this.A = A;
		refreshCoefs();
	}

	public void setB(Point B) {
		this.B = B;
		refreshCoefs();
	}

	/**
	 * Nalezne průsečík této a jiné přímky
	 *
	 * @param line	druhá přímka
	 * @return	bod, kde se protnou, nebo null, pokud se neprotnou
	 */
	public Point intersectPoint(Line line) {
		double denominator = (a * line.b - line.a * b);
		if (denominator == 0) {
			return null;
		}
		return new Point((b * line.c - c * line.b) / denominator, -(a * line.c - line.a * c) / denominator);
	}

	/**
	 * Vypočítá úhel, který svírají tato přímka s další
	 *
	 * @param line	druhá přímka
	 * @return	úhel v radiánech v protisměru hodinových ručiček, který tato přímka svírá s druhou dodanou
	 */
	public double getAngle(Line line) {
		return Math.acos((a * line.a + b * line.b) / (Math.sqrt(a * a + b * b) * Math.sqrt(line.a * line.a + line.b * line.b)));
	}

	/**
	 * Vytvoří obraz réro přímky souměrný přes osu danou přímkou
	 *
	 * @param mirror	osa souměrnosti
	 * @return	obraz přes osu souměrnosti mirror
	 */
	public Line mirrorReflection(Line mirror) {
		Point intersect = this.intersectPoint(mirror);
		Line lineNormal = constructFromPointAndVector(intersect, mirror.a, mirror.b);
		Line lineParalell = constructFromPointAndNormal(A, mirror.a, mirror.b);
		Point S = lineParalell.intersectPoint(lineNormal);
		Point reflectedA = new Point(2 * S.x - A.x, 2 * S.y - A.y);
		return Line.constructFromTwoPoints(intersect, reflectedA);
	}

	public boolean isPointOnSegment(Point p) {
		double vx = B.x - A.x;
		double vy = B.y - A.y;
		if (Math.abs(vx) > Math.abs(vy)) {
			return (p.x - A.x) / vx >= 0 && (p.x - A.x) / vx <= 1;
		} else {
			return (p.y - A.y) / vy >= 0 && (p.y - A.y) / vy <= 1;
		}
	}
	
	public boolean isPointOnRay(Point p) {
		double vx = B.x - A.x;
		double vy = B.y - A.y;
		if (Math.abs(vx) > Math.abs(vy)) {
			return (p.x - A.x) / vx >= 0;
		} else {
			return (p.y - A.y) / vy >= 0;
		}		
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Line");
		sb.append(this.A.toString());
		sb.append(";");
		sb.append(B.toString());
		sb.append("\n");
		return sb.toString();
	}
}
