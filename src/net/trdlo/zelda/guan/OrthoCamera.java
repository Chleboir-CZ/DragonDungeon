package net.trdlo.zelda.guan;

import net.trdlo.zelda.XY;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import net.trdlo.zelda.NU;

class OrthoCamera {

	public static final double ZOOM_BASE = 1.090507733; //2^(1/8)
	private static final Stroke DASHED_STROKE = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0);

	private boolean boundsDebug = false;

	private World world;
	private double x, y;
	private int zoom;

	private double zoomCoefLimit;
	private Rectangle componentBounds, cameraBounds;

	private Point dragStart, dragEnd;
	private boolean snapToGrid = true;
	private Point moveStart, moveEnd, snappedPoint, moveDiff;
	private Point nearestPoint;
	private final Set<Point> selection;
	private Set<Point> tempSelection;
	private boolean additiveSelection;

	private Line selectedLine, nearestLine;

	private int gridDensity = 6;
	private int gridStep = 64;

	public OrthoCamera(World world, double x, double y, int zoom) {
		setWorld(world, x, y, zoom);

		selection = new HashSet<>();
		tempSelection = new HashSet<>();
	}

	public final void setWorld(World world, double x, double y, int zoom) {
		this.world = world;
		this.x = x;
		this.y = y;
		this.zoom = zoom;
	}

	public void update() {

	}

	private int worldToViewX(double x) {
		assert componentBounds != null;
		return (int) ((componentBounds.width / 2) + (x - this.x) * zoomCoef());
	}

	public int worldToViewY(double y) {
		assert componentBounds != null;
		return (int) ((componentBounds.height / 2) + (y - this.y) * zoomCoef());
	}

	public XY worldToView(Point p) {
		return new XY(worldToViewX(p.x), worldToViewY(p.y));
	}

	public double viewToWorldX(int x) {
		assert componentBounds != null;
		return (x - (componentBounds.width / 2)) / zoomCoef() + this.x;
	}

	public double viewToWorldY(int y) {
		assert componentBounds != null;
		return (y - (componentBounds.height / 2)) / zoomCoef() + this.y;
	}

	public Point viewToWorld(XY xy) {
		return new Point(viewToWorldX(xy.x), viewToWorldY(xy.y));
	}

	private void renderGrid(Graphics2D graphics) {
		if (gridDensity >= 0) {
			graphics.setColor(Color.DARK_GRAY);

			int left = (int) NU.ceilToMultipleOf(viewToWorldX(0), gridStep);
			int right = (int) NU.floorToMultipleOf(viewToWorldX(componentBounds.width), gridStep);
			for (int i = left; i <= right; i += gridStep) {
				int vx = worldToViewX(i);
				graphics.drawLine(vx, 0, vx, componentBounds.height);
			}

			int top = (int) NU.ceilToMultipleOf(viewToWorldY(0), gridStep);
			int bottom = (int) NU.floorToMultipleOf(viewToWorldY(componentBounds.height), gridStep);
			for (int i = top; i <= bottom; i += (int) Math.pow(2, gridDensity)) {
				int vy = worldToViewY(i);
				graphics.drawLine(0, vy, componentBounds.width, vy);
			}
		}
	}

	private static final Stroke DEFAULT_STROKE = new BasicStroke(1);
	private static final Stroke SELECTION_STROKE = new BasicStroke(2);

	private void renderBoundsDebug(Graphics2D graphics) {
		if (world.bounds == null) {
			graphics.setColor(Color.RED);
			graphics.drawString("Bounds not set!", 50, 10);
			graphics.setColor(Color.WHITE);
			return;
		}
		graphics.drawString(String.format("Zoom = %f", zoomCoef()), 50, 20);

		graphics.setStroke(SELECTION_STROKE);
		graphics.setColor(Color.PINK);
		int l = worldToViewX(world.bounds.x), t = worldToViewY(world.bounds.y), w = worldToViewX(world.bounds.x + world.bounds.width) - l, h = worldToViewY(world.bounds.y + world.bounds.height) - t;
		graphics.drawRect(l, t, w, h);
		if (cameraBounds != null) {
			graphics.setStroke(DEFAULT_STROKE);
			graphics.setColor(Color.RED);
			graphics.drawRect(worldToViewX(x) - Point.DISPLAY_SIZE / 2, worldToViewY(y) - Point.DISPLAY_SIZE / 2, Point.DISPLAY_SIZE, Point.DISPLAY_SIZE);
			l = worldToViewX(cameraBounds.x);
			t = worldToViewY(cameraBounds.y);
			w = worldToViewX(cameraBounds.x + cameraBounds.width) - l;
			h = worldToViewY(cameraBounds.y + cameraBounds.height) - t;
			if (w > 0 && h > 0) {
				graphics.drawRect(l, t, w, h);
			} else if (w > 0) {
				t = worldToViewY(cameraBounds.y + cameraBounds.height / 2);
				graphics.drawLine(l, t, l + w, t);
			} else if (h > 0) {
				l = worldToViewX(cameraBounds.x + cameraBounds.width / 2);
				graphics.drawLine(l, t, l, t + h);
			}

		}
	}

	private void renderReflectionsDebug(Graphics2D graphics) {
		if (world.lines.size() < 2) {
			return;
		}
		Iterator<Line> it = world.lines.iterator();
		Line t = it.next(), m = it.next();

		graphics.setStroke(DASHED_STROKE);
		graphics.setColor(Color.DARK_GRAY);

		Line r = m.reflect(t);
		graphics.drawLine(worldToViewX(r.A.x), worldToViewY(r.A.y), worldToViewX(r.B.x), worldToViewY(r.B.y));
		Line b = m.bounceOff(t);
		if (b != null) {
			graphics.drawLine(worldToViewX(b.A.x), worldToViewY(b.A.y), worldToViewX(b.B.x), worldToViewY(b.B.y));
		}
		Line br = m.bounceOffRay(t);
		if (br != null) {
			graphics.drawLine(worldToViewX(br.A.x), worldToViewY(br.A.y), worldToViewX(br.B.x), worldToViewY(br.B.y));
		}
	}

	private void renderCrossingDebug(Graphics2D graphics, Line line) {
		if ((System.nanoTime() / 500000000L & 1) == 0) {
			return;
		}

		Point iP;
		for (Line cross : world.lines) {
			if (cross != line && (iP = cross.getSegmentSegmentIntersection(line)) != null) {
				XY iP1 = worldToView(iP);
				graphics.setColor(Color.RED);
				graphics.drawArc(iP1.x - 8, iP1.y - 8, 16, 16, 0, 360);
			}
		}
	}

	public void render(Graphics2D graphics, Rectangle componentBounds, float renderFraction) {
		this.componentBounds = componentBounds;

		if (boundsDebug) {
			renderBoundsDebug(graphics);
		}

		renderGrid(graphics);

		double dx = 0, dy = 0;
		if (moveStart != null && moveEnd != null) {
			dx = moveEnd.getX() - moveStart.getX();
			dy = moveEnd.getY() - moveStart.getY();
		}

		graphics.setStroke(Line.DEFAULT_STROKE);
		graphics.setColor(Line.DEFAULT_COLOR);
		for (Line line : world.lines) {
			double lAx = line.getA().x, lAy = line.getA().y;
			double lBx = line.getB().x, lBy = line.getB().y;
			if (selection.contains(line.getA())) {
				lAx += dx;
				lAy += dy;
			}
			if (selection.contains(line.getB())) {
				lBx += dx;
				lBy += dy;
			}

			if (line == selectedLine) {
				graphics.setColor(Line.SELECTION_COLOR);
				graphics.setStroke(Line.SELECTION_STROKE);
			} else {
				graphics.setColor(Line.DEFAULT_COLOR);
				graphics.setStroke(Line.DEFAULT_STROKE);
			}

			if (line == nearestLine) {
				graphics.setColor(Color.YELLOW);
			}

			graphics.drawLine(worldToViewX(lAx), worldToViewY(lAy), worldToViewX(lBx), worldToViewY(lBy));
			renderCrossingDebug(graphics, line);
		}

		for (Point point : world.points) {
			double px = point.x, py = point.y;
			if (selection.contains(point) || tempSelection.contains(point)) {
				graphics.setStroke(Point.SELECTION_STROKE);
				graphics.setColor(Point.SELECTION_COLOR);
				px += dx;
				py += dy;
			} else if (point == nearestPoint) {
				graphics.setStroke(Point.DEFAULT_STROKE);
				graphics.setColor(Point.SELECTION_COLOR);
			} else {
				graphics.setStroke(Point.DEFAULT_STROKE);
				graphics.setColor(Point.DEFAULT_COLOR);
			}
			int vx = worldToViewX(px);
			int vy = worldToViewY(py);
			graphics.drawRect(vx - Point.DISPLAY_SIZE / 2, vy - Point.DISPLAY_SIZE / 2, Point.DISPLAY_SIZE, Point.DISPLAY_SIZE);
			graphics.drawString(point.getDescription(), vx + Point.DISPLAY_SIZE, vy + Point.DISPLAY_SIZE);
		}

		renderReflectionsDebug(graphics);

		if (dragStart != null && dragEnd != null) {
			graphics.setStroke(DASHED_STROKE);
			graphics.setColor(Color.LIGHT_GRAY);
			graphics.drawRect(
					worldToViewX(Math.min(dragStart.getX(), dragEnd.getX())),
					worldToViewY(Math.min(dragStart.getY(), dragEnd.getY())),
					(int) (Math.abs(dragStart.getX() - dragEnd.getX()) * zoomCoef()),
					(int) (Math.abs(dragStart.getY() - dragEnd.getY()) * zoomCoef()));
		}
	}

	private double zoomCoef() {
		return Math.max(zoomCoefLimit, Math.pow(ZOOM_BASE, zoom));
	}

	public void zoom(int change, XY fixedPoint) {
		if (componentBounds == null) {
			return;
		}

		double wx = x;
		double wy = y;
		if (fixedPoint != null) {
			wx = viewToWorldX(fixedPoint.x);
			wy = viewToWorldY(fixedPoint.y);
		}

		double c1 = zoomCoef();
		if (zoomCoefLimit < c1 || change > 0) {
			zoom += change;
		}
		double coefChange = zoomCoef() / c1;

		double dx = (wx - x) / coefChange;
		double dy = (wy - y) / coefChange;

		x = wx - dx;
		y = wy - dy;
		checkBounds();
	}

	public void move(XY diff) {
		x += diff.x / zoomCoef();
		y += diff.y / zoomCoef();
		checkBounds();
	}

	public void checkBounds() {
		if (world.bounds == null) {
			return;
		}

		zoomCoefLimit = Math.min(componentBounds.width / (double) world.bounds.width, componentBounds.height / (double) world.bounds.height);

		double left = world.bounds.x + (componentBounds.width / 2) / zoomCoef();
		double right = world.bounds.x + world.bounds.width - (componentBounds.width / 2) / zoomCoef();
		double top = world.bounds.y + (componentBounds.height / 2) / zoomCoef();
		double bottom = world.bounds.y + world.bounds.height - (componentBounds.height / 2) / zoomCoef();

		cameraBounds = new Rectangle((int) left, (int) top, (int) (right - left), (int) (bottom - top));

		if (left < right) {
			if (x < left) {
				x = left;
			}
			if (x > right) {
				x = right;
			}
		} else {
			x = world.bounds.x + world.bounds.width / 2;
		}
		if (top < bottom) {
			if (y < top) {
				y = top;
			}
			if (y > bottom) {
				y = bottom;
			}
		} else {
			y = world.bounds.y + world.bounds.height / 2;
		}
	}

	public boolean isBoundsDebug() {
		return boundsDebug;
	}

	public void setBoundsDebug(boolean showBoundsDebug) {
		this.boundsDebug = showBoundsDebug;
	}

	public Point getPointAt(int x, int y) {
		return world.getPointAt(viewToWorldX(x), viewToWorldY(y), Point.DISPLAY_SIZE / zoomCoef());
	}

	public Line getLineAt(int x, int y) {
		return world.getLineAt(viewToWorldX(x), viewToWorldY(y), Line.SELECTION_MAX_DISTANCE / zoomCoef());
	}

	public Set<Point> getPointsIn(int x1, int y1, int x2, int y2) {
		return world.getPointsIn(viewToWorldX(x1), viewToWorldY(y1), viewToWorldX(x2), viewToWorldY(y2));
	}

	public void mouse1pressed(MouseEvent e) {
		additiveSelection = e.isShiftDown();
		Point pointAt;
		Line lineAt;
		if ((pointAt = getPointAt(e.getX(), e.getY())) != null) {
			if (additiveSelection && selection.contains(pointAt)) {
				selection.remove(pointAt);
			} else {
				if (!additiveSelection && !selection.contains(pointAt)) {
					selection.clear();
				}
				selection.add(pointAt);
				moveStart = new Point(viewToWorldX(e.getX()), viewToWorldY(e.getY()));
				snappedPoint = pointAt;
				moveDiff = moveStart.diff(snappedPoint);
			}
			selectedLine = null;
		} else if (!additiveSelection && (lineAt = getLineAt(e.getX(), e.getY())) != null) {
			selection.clear();
			selectedLine = lineAt;
		} else {
			dragStart = new Point(viewToWorldX(e.getX()), viewToWorldY(e.getY()));
			selectedLine = null;
		}
	}

	private void snapp() {
		if (!snapToGrid || gridDensity == -1) {
			return;
		}

		moveEnd.roundToGrid(gridStep);
		moveEnd.moveBy(moveDiff);
	}

	public void mouseMoved(MouseEvent e) {
		nearestPoint = world.getPointAt(viewToWorldX(e.getX()), viewToWorldY(e.getY()), 64 / zoomCoef());
		nearestLine = world.getLineAt(viewToWorldX(e.getX()), viewToWorldY(e.getY()), 64 / zoomCoef());
	}

	public void mouse1dragged(MouseEvent e) {
		if (dragStart != null) {
			if (dragEnd == null) {
				dragEnd = new Point();
			}
			dragEnd.setX(viewToWorldX(e.getX()));
			dragEnd.setY(viewToWorldY(e.getY()));

			tempSelection = world.getPointsIn(dragStart.getX(), dragStart.getY(), dragEnd.getX(), dragEnd.getY());
			if (!additiveSelection) {
				selection.clear();
			}
		} else if (moveStart != null) {
			if (moveEnd == null) {
				moveEnd = new Point();
			}
			moveEnd.setX(viewToWorldX(e.getX()));
			moveEnd.setY(viewToWorldY(e.getY()));
			snapp();
		}
	}

	public void mouse1released(MouseEvent e) {
		if (dragStart != null) {
			if (!additiveSelection) {
				selection.clear();
			}
			if (dragEnd != null) {
				selection.addAll(tempSelection);
				tempSelection.clear();
				dragEnd = null;
			}
			dragStart = null;
		} else if (moveStart != null) {
			if (moveEnd != null) {
				world.shiftPoints(selection, moveEnd.getX() - moveStart.getX(), moveEnd.getY() - moveStart.getY());
				moveEnd = null;
			}
			moveStart = null;
		}
	}

	public void deleteSelection() {
		if (selectedLine != null) {
			world.deleteLine(selectedLine);
		} else {
			world.deletePoints(selection);
		}
	}

	public void nextGridDensity() {
		gridDensity--;
		if (gridDensity == -1) {
			//Console.getInstance().echo(2000, "Grid off");
		} else if (gridDensity == -2) {
			gridDensity = 8;
		} //Console.getInstance().echo(2000, "Grid size %d world units", (int) Math.pow(2, gridDensity));

		gridStep = (int) Math.pow(2, gridDensity);
	}

	public void toggleSnapToGrid() {
		snapToGrid = !snapToGrid;
	}

}