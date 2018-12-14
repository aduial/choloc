package choloc.app.viewer;

import java.awt.BorderLayout;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;

/**
 * Hello world!
 *
 */
public class MapViewerApplication {

	public static void main(String[] args) {

		// Create frame
		final JFrame frame = new JFrame();
		frame.setLayout(new BorderLayout());
		
		// Create a text field for the information
		final JTextArea infoText = new JTextArea();

		// Create map viewer
		final MapViewer mapViewer = new MapViewer(infoText::setText);

		// create a button for finding objects
		final JButton findObjectsButton = new JButton("Find Objects");
		findObjectsButton.addActionListener(event -> mapViewer.scoutSurroundings());

		// Set up panel for map and button
		final JPanel leftPanel = new JPanel(new BorderLayout());
		leftPanel.add(mapViewer, BorderLayout.CENTER);
		leftPanel.add(findObjectsButton, BorderLayout.SOUTH);
		
		// Set up split panel
		final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, infoText);
		splitPane.setResizeWeight(0.5);
		frame.add(splitPane,BorderLayout.CENTER);
		
		// Show
		frame.pack();
		frame.setVisible(true);
	}
}
