package net.trdlo.zelda.notiles;

import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.trdlo.zelda.ZWorld;
import net.trdlo.zelda.exceptions.ZException;

/**
 *
 * @author chleboir
 */
public class World extends ZWorld {

	private class UnregisteringLineList extends ArrayList<Line> {

		@Override
		public boolean remove(Object o) {
			if (o instanceof Line) {
				((Line) o).unregister();
			}
			return super.remove(o);
		}
	}

	List<Line> lines;
	List<Point> points;

	//dočasný pokusný paprsek, časem pude do kšá
	Line ray;

	private World() {
		lines = new UnregisteringLineList();
		points = new ArrayList<>();
		Point.setLinesCollection(lines);

		ray = Line.constructFromTwoPoints(new Point(500, 400), new Point(180, 100));
	}

	public static World createTestWorld() {
		World world = new World();

		world.points.add(new Point(200, 200));
		world.points.add(new Point(200, 390));
		world.points.add(new Point(407, 400));
		world.points.get(0).lineTo(world.points.get(1)).lineTo(world.points.get(2));
		return world;
	}

	public static World loadWorldFromReader(BufferedReader reader) throws ZException {
		String inputLine;
		World world = new World();
		Pattern pointPattern = Pattern.compile("^\\s*point\\s*(\\d+)\\s*\\[\\s*([-+]?\\d*\\.?\\d+)\\s*;\\s*([-+]?\\d*\\.?\\d+)\\s*\\]\\s*(.*)$", Pattern.CASE_INSENSITIVE);
		Pattern linePattern = Pattern.compile("^\\s*line\\s*(\\d+)\\s*(\\d+)\\s*$", Pattern.CASE_INSENSITIVE);
		Map<Integer, Point> pointMap = new HashMap<>();
		try {
			while ((inputLine = reader.readLine()) != null) {
				Matcher matcher;
				if ((matcher = pointPattern.matcher(inputLine)).matches()) {
					Point p = new Point(Double.valueOf(matcher.group(2)), Double.valueOf(matcher.group(3)), matcher.group(4));
					pointMap.put(Integer.valueOf(matcher.group(1)), p);

					world.points.add(p);
				} else if ((matcher = linePattern.matcher(inputLine)).matches()) {
					Point A = pointMap.get(Integer.valueOf(matcher.group(1)));
					Point B = pointMap.get(Integer.valueOf(matcher.group(2)));

					if (A == null || B == null) {
						throw new ZException("Invalid input format.");
					}
					Line line = Line.constructFromTwoPoints(A, B);
					world.lines.add(line);
				}
			}
		} catch (IOException ex) {
			throw new ZException("Could not load. IO error occured", ex);
		}
		return world;
	}

	@Override
	public void update() {
		//independentPoints.get(0).y += 1;
	}

	public Point getPointAt(int x, int y) {
		for (Point p : this.points) {
			if (Math.abs(p.x - x) < View.POINT_DISPLAY_SIZE && Math.abs(p.y - y) < View.POINT_DISPLAY_SIZE) {
				return p;
			}
		}
		return null;
	}

	/**
	 * TODO: zobecnit na 4úhelník! Bude potřeba, pokud bude View perspektiva Vrátí kolekci bodů, která je uvnitř obdélníku zadaného dvěma protilehlými rohy
	 *
	 * @param A	jeden roh
	 * @param B	druhý roh
	 * @return	kolekce bodů světa, které jsou uvnitř obdélníku
	 */
	public Collection<Point> pointsInRect(Point A, Point B) {
		Collection<Point> pointsInRect = new ArrayList<>();

		Rectangle rect = new Rectangle(A.getJavaPoint());
		rect.add(B.getJavaPoint());

		for (Point p : points) {
			if (rect.contains(p.getJavaPoint())) {
				pointsInRect.add(p);
			}
		}
		return pointsInRect;
	}

	/**
	 * Odebere bod ze světa a dle jeho seznamu listenerů i napojené lajny
	 *
	 * @param point	bod k odebrání
	 */
	public void removePoint(Point point) {
		point.setIgnoreUnregisters();
		for (Line l : point.changeListeners) {
			lines.remove(l);
		}
		points.remove(point);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < points.size(); i++) {
//			sb.append(i);
			sb.append(points.get(i).toString());
			sb.append("\n");
			i++;
		}

		for (Line l : lines) {
			sb.append("Line ");
			sb.append(points.indexOf(l.A)).append(" ").append(points.indexOf(l.B));
			sb.append("\n");
		}
		return sb.toString();
	}

//	public static World fromString(String s) {
//		World world = new World();
//		Pattern intPattern = Pattern.compile("");
//		Pattern descrPattern = Pattern.compile("\".*\"");
//		String[] stgArray = s.split("\n");
//		for(int i = 0; i < stgArray.length; i++) {
//			Matcher intMatcher = intPattern.matcher(stgArray[i]);
//			
////			String[] readLine = stgArray[i].split(" ");
////			if("Point".equals(readLine[0])) {
////				readLine[1] = readLine[1].replaceAll("\\D+", " ");
////				world.points.add(new Point(Integer.parseInt(readLine[1].split(" ")[0]), Integer.parseInt(readLine[1].split(" ")[1]), readLine[2]));
//		}
//	}
	@Override
	protected void saveToWriter(BufferedWriter writer) throws ZException {
		try {
			int i = 0;
			for (Point p : points) {
				p.saveToWriter(writer, i++);
				writer.write("\n");
			}

			for (Line l : lines) {
				writer.write("Line ");
				writer.write(points.indexOf(l.A) + " " + points.indexOf(l.B));
				writer.write("\n");
			}
		} catch (IOException ex) {
			throw new ZException("An IO exception occured.", ex);
		}
	}

}
