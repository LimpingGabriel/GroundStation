package application.map;

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.util.LatLongUtils;
import org.mapsforge.core.util.Parameters;
import org.mapsforge.map.awt.graphics.AwtGraphicFactory;
import org.mapsforge.map.awt.util.AwtUtil;
import org.mapsforge.map.awt.util.JavaPreferences;
import org.mapsforge.map.awt.view.MapView;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.datastore.MultiMapDataStore;
import org.mapsforge.map.layer.Layers;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.debug.TileCoordinatesLayer;
import org.mapsforge.map.layer.debug.TileGridLayer;
import org.mapsforge.map.layer.download.TileDownloadLayer;
import org.mapsforge.map.layer.download.tilesource.OpenStreetMapMapnik;
import org.mapsforge.map.layer.download.tilesource.TileSource;
import org.mapsforge.map.layer.hills.DiffuseLightShadingAlgorithm;
import org.mapsforge.map.layer.hills.HillsRenderConfig;
import org.mapsforge.map.layer.hills.MemoryCachingHgtReaderTileSource;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.model.IMapViewPosition;
import org.mapsforge.map.model.Model;
import org.mapsforge.map.model.common.PreferencesFacade;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.InternalRenderTheme;

import javafx.embed.swing.SwingNode;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.StackPane;

import javax.swing.*;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.prefs.Preferences;

public class JFXOpenStreetMap extends StackPane {
	//----------------FXML-----------------
	
	@FXML private SwingNode swingNode;
	
	
    private static final GraphicFactory GRAPHIC_FACTORY = AwtGraphicFactory.INSTANCE;
    private static final boolean SHOW_DEBUG_LAYERS = false;
    private static final boolean SHOW_RASTER_MAP = false;
    
    public JFXOpenStreetMap() {
    	//FXML
    	
    	
    	FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("JFXOpenStreetMap.fxml"));
    	fxmlLoader.setRoot(this);
    	fxmlLoader.setController(this);
    	try {
    		fxmlLoader.load();
    	} catch (IOException e) {
    		e.printStackTrace();
    	}
    	
    	
    	
        // Square frame buffer
        Parameters.SQUARE_FRAME_BUFFER = false;

        HillsRenderConfig hillsCfg = null;


        List<File> mapFiles = SHOW_RASTER_MAP ? null : getMapFiles();
        final MapView mapView = createMapView();
        final BoundingBox boundingBox = addLayers(mapView, mapFiles, hillsCfg);

        final PreferencesFacade preferencesFacade = new JavaPreferences(Preferences.userNodeForPackage(JFXOpenStreetMap.class));

        JPanel panel = new JPanel(new BorderLayout());

        final Model model = mapView.getModel();
        model.init(preferencesFacade);
        if (model.mapViewPosition.getZoomLevel() == 0 || !boundingBox.contains(model.mapViewPosition.getCenter())) {
            byte zoomLevel = LatLongUtils.zoomForBounds(model.mapViewDimension.getDimension(), boundingBox, model.displayModel.getTileSize());
            model.mapViewPosition.setMapPosition(new MapPosition(boundingBox.getCenterPoint(), zoomLevel));
        }
        
        panel.add(mapView);
        swingNode.setContent(panel);
    }
    
    private static BoundingBox addLayers(MapView mapView, List<File> mapFiles, HillsRenderConfig hillsRenderConfig) {
        Layers layers = mapView.getLayerManager().getLayers();

        int tileSize = SHOW_RASTER_MAP ? 256 : 512;

        // Tile cache
        TileCache tileCache = AwtUtil.createTileCache(
                tileSize,
                mapView.getModel().frameBufferModel.getOverdrawFactor(),
                1024,
                new File(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString()));

        final BoundingBox boundingBox;
        if (SHOW_RASTER_MAP) {
            // Raster
            mapView.getModel().displayModel.setFixedTileSize(tileSize);
            OpenStreetMapMapnik tileSource = OpenStreetMapMapnik.INSTANCE;
            tileSource.setUserAgent("mapsforge-samples-awt");
            TileDownloadLayer tileDownloadLayer = createTileDownloadLayer(tileCache, mapView.getModel().mapViewPosition, tileSource);
            layers.add(tileDownloadLayer);
            tileDownloadLayer.start();
            mapView.setZoomLevelMin(tileSource.getZoomLevelMin());
            mapView.setZoomLevelMax(tileSource.getZoomLevelMax());
            boundingBox = new BoundingBox(LatLongUtils.LATITUDE_MIN, LatLongUtils.LONGITUDE_MIN, LatLongUtils.LATITUDE_MAX, LatLongUtils.LONGITUDE_MAX);
        } else {
            // Vector
            mapView.getModel().displayModel.setFixedTileSize(tileSize);
            MultiMapDataStore mapDataStore = new MultiMapDataStore(MultiMapDataStore.DataPolicy.RETURN_ALL);
            for (File file : mapFiles) {
                mapDataStore.addMapDataStore(new MapFile(file), false, false);
            }
            TileRendererLayer tileRendererLayer = createTileRendererLayer(tileCache, mapDataStore, mapView.getModel().mapViewPosition, hillsRenderConfig);
            layers.add(tileRendererLayer);
            boundingBox = mapDataStore.boundingBox();
        }

        // Debug
        if (SHOW_DEBUG_LAYERS) {
            layers.add(new TileGridLayer(GRAPHIC_FACTORY, mapView.getModel().displayModel));
            layers.add(new TileCoordinatesLayer(GRAPHIC_FACTORY, mapView.getModel().displayModel));
        }

        return boundingBox;
    }

    private static MapView createMapView() {
        MapView mapView = new MapView();
        mapView.getMapScaleBar().setVisible(true);
        if (SHOW_DEBUG_LAYERS) {
            mapView.getFpsCounter().setVisible(true);
        }

        return mapView;
    }

    @SuppressWarnings("unused")
    private static TileDownloadLayer createTileDownloadLayer(TileCache tileCache, IMapViewPosition mapViewPosition, TileSource tileSource) {
        return new TileDownloadLayer(tileCache, mapViewPosition, tileSource, GRAPHIC_FACTORY) {
            @Override
            public boolean onTap(LatLong tapLatLong, Point layerXY, Point tapXY) {
                System.out.println("Tap on: " + tapLatLong);
                return true;
            }
        };
    }

    private static TileRendererLayer createTileRendererLayer(TileCache tileCache, MapDataStore mapDataStore, IMapViewPosition mapViewPosition, HillsRenderConfig hillsRenderConfig) {
        TileRendererLayer tileRendererLayer = new TileRendererLayer(tileCache, mapDataStore, mapViewPosition, false, true, false, GRAPHIC_FACTORY, hillsRenderConfig) {
            @Override
            public boolean onTap(LatLong tapLatLong, Point layerXY, Point tapXY) {
                System.out.println("Tap on: " + tapLatLong);
                return true;
            }
        };
        tileRendererLayer.setXmlRenderTheme(InternalRenderTheme.DEFAULT);
        return tileRendererLayer;
    }

    private static File getDemFolder(String[] args) {
    	/*
        if (args.length == 0) {
            if (SHOW_RASTER_MAP) {
                return null;
            } else {
                throw new IllegalArgumentException("missing argument: <mapFile>");
            }
        }

        File demFolder = new File(args[0]);
        if (demFolder.exists() && demFolder.isDirectory() && demFolder.canRead()) {
            return demFolder;
        }
        */
        return null;
    }


    private static List<File> getMapFiles() {
    	/*
        if (args.length == 0) {
            throw new IllegalArgumentException("missing argument: <mapFile>");
        }
        */

        List<File> result = new ArrayList<>();
        
        result.add(new File("C:\\Users\\Benjamin\\Downloads\\quebec.map"));
        result.add(new File("C:\\Users\\Benjamin\\Downloads\\prince-edward-island.map"));
        result.add(new File("C:\\Users\\Benjamin\\Downloads\\us-northeast.map"));
        return result;
    }
    
    
    @FXML protected void increaseZoom(ActionEvent actionEvent) {
    	
    }
    
    @FXML protected void decreaseZoom(ActionEvent actionEvent) {
    	
    }
}