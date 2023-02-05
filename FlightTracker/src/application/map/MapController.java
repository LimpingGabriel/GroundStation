package application.map;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public class MapController {
	@FXML private MapWidget offlineMapWidget;
	
	@FXML protected void zoomInMap(ActionEvent event) {
		offlineMapWidget.increaseZoom();
		offlineMapWidget.drawMap();
	}
	
	@FXML protected void zoomOutMap(ActionEvent event) {
		offlineMapWidget.decreaseZoom();
		offlineMapWidget.drawMap();
	}
	
}
