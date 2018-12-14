package choloc.app.viewer;

import java.awt.Color;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.ParserConfigurationException;

import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.MapMarkerDot;
import org.openstreetmap.gui.jmapviewer.interfaces.ICoordinate;
import org.openstreetmap.gui.jmapviewer.interfaces.MapMarker;
import org.xml.sax.SAXException;

import choloc.app.streetfinder.Street;
import choloc.app.streetfinder.StreetFinder;

public class MapViewer extends JMapViewer {

	private static final long serialVersionUID = 8678292754866282345L;

	private transient MapMarkerDot myPositionMarker = null;
	private transient List<ContentPosition> contentPositions = Collections.emptyList();

	private final transient Consumer<String> resultSetter;

	public MapViewer(Consumer<String> resultSetter) {

		this.resultSetter = resultSetter;

		// Create a click event for clicking on the map
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				setPosition(e.getPoint());
				super.mouseClicked(e);
			}
		});
	}

	private void setPosition(Point clicked) {

		// Find clicked position (last position in list is top)
		ContentPosition clickedPosition = null;
		for (ContentPosition candidate : this.contentPositions) {
			if (candidate.isInMarker(clicked, this::getMapPosition)) {
				clickedPosition = candidate;
			}
		}

		// Handle clicked position
		if (clickedPosition != null) {
			// Set the text of the clicked position.
			this.resultSetter.accept(clickedPosition.getPosition().getName());
		} else {
			// Change my position.
			final ICoordinate coordinate = getPosition(clicked);
			if (myPositionMarker != null) {
				removeMapMarker(myPositionMarker);
			}
			myPositionMarker = new MapMarkerDot(null, "My position", clone(coordinate));
			myPositionMarker.setBackColor(Color.RED);
			addMapMarker(myPositionMarker);
			this.resultSetter.accept("");
		}
	}

	public ICoordinate getCurrentPosition() {
		return clone(this.myPositionMarker.getCoordinate());
	}

	public void scoutSurroundings() {

		// Maybe no position has been set.
		if (myPositionMarker == null) {
			return;
		}

		// Obtain data
		final Coordinate here = clone(myPositionMarker.getCoordinate());
		final List<Street> streetResults;
		try {
			streetResults = new StreetFinder().findStreetsSortedByDistance(here.getLat(), here.getLon(), 500);
		} catch (TransformException | IOException | ParserConfigurationException | SAXException | FactoryException e) {
			e.printStackTrace();
			return;
		}

		// Create list of content positions.
		contentPositions = streetResults.stream().map(street -> new ContentPosition(street, null))
				.collect(Collectors.toList());

		// Remove old positions
		removeAllMapMarkers();

		// Set my current marker to green
		myPositionMarker.setBackColor(Color.GREEN);

		// Add new markers
		final List<MapMarker> markers = Stream
				.concat(Stream.of(myPositionMarker), contentPositions.stream().map(ContentPosition::getPosition))
				.collect(Collectors.toList());
		setMapMarkerList(markers);
	}

	static final Coordinate clone(ICoordinate coordinate) {
		return new Coordinate(coordinate.getLat(), coordinate.getLon());
	}

	private static class ContentPosition {

		private final MapMarkerDot position;
		private final String content;

		public ContentPosition(Street street, String content) {
			final boolean hasContent = content != null && !content.trim().isEmpty();
			this.position = new MapMarkerDot(null, street.getStreetName(),
					new Coordinate(street.getLat(), street.getLon()));
			this.position.setBackColor(hasContent ? Color.YELLOW : Color.WHITE);
			this.content = hasContent ? content : "No content on this position";
		}

		public MapMarkerDot getPosition() {
			return position;
		}

		public String getContent() {
			return content;
		}

		public boolean isInMarker(Point point, Function<Coordinate, Point> toMapPointConverter) {
			final Point markerCenter = toMapPointConverter.apply(position.getCoordinate());
			return markerCenter != null && markerCenter.distance(point) < position.getRadius();
		}
	}
}