package application.map;
import application.ResizableCanvas;

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.awt.graphics.AwtGraphicFactory;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.layer.cache.InMemoryTileCache;
import org.mapsforge.map.layer.labels.TileBasedLabelStore;
import org.mapsforge.map.layer.renderer.DatabaseRenderer;
import org.mapsforge.map.layer.renderer.RendererJob;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.model.FixedTileSizeDisplayModel;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.InternalRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderTheme;
import org.mapsforge.map.rendertheme.rule.RenderThemeFuture;

import org.mapsforge.map.layer.renderer.TileRendererLayer;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * This sample demo how to render & save a tile.
 */
public class MapWidget extends StackPane {
	@FXML private ResizableCanvas canvas;
	
	private GraphicsContext gc;
	private MapDataStore mapData;
	private GraphicFactory gf;	
	private XmlRenderTheme theme;
    private DisplayModel dm;
    private RenderThemeFuture rtf;
    private InMemoryTileCache tileCache;
    private TileBasedLabelStore tileBasedLabelStore;
    private DatabaseRenderer renderer;
    private TileRendererLayer tileRendererLayer;

    // Location you'd like to render.
    private static final double LAT = 46.227220;
    private static final double LNG = -63.140568;
    private byte ZOOM = 16;
    private static final int TILE_WIDTH = 256;
    
    
    
    private static final String DEFAULT_MAP_PATH = "C:\\Users\\Benjamin\\OneDrive\\University\\Undergraduate\\MRT\\GroundStation\\FlightTracker\\bin\\application\\prince-edward-island.map";
	
	public MapWidget() {
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("MapWidget.fxml"));
		fxmlLoader.setRoot(this);
		fxmlLoader.setController(this);
		try {
			fxmlLoader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		this.mapData = new MapFile(DEFAULT_MAP_PATH);
		this.gf = AwtGraphicFactory.INSTANCE;
		this.theme = InternalRenderTheme.OSMARENDER;
		this.dm = new FixedTileSizeDisplayModel(this.TILE_WIDTH);
		this.rtf = new RenderThemeFuture(gf, theme, dm);
		
        // Create RendererTheme.
        Thread t = new Thread(rtf);
        t.start();
		
        this.tileCache = new InMemoryTileCache(256);
        this.tileBasedLabelStore = new TileBasedLabelStore(this.tileCache.getCapacityFirstLevel());
        this.renderer = new DatabaseRenderer(this.mapData, this.gf, this.tileCache, this.tileBasedLabelStore, true, true, null);
		
		//Canvas resizing
		this.widthProperty().addListener(e -> this.resizeCallback());
		this.heightProperty().addListener(e -> this.resizeCallback());
		
		this.gc = this.canvas.getGraphicsContext2D();
		
	}
	
	private void resizeCallback() {
		this.canvas.resize(this.getWidth(), this.getHeight());
		this.drawMap();
	}
	
	@FXML protected void zoomInMap(ActionEvent event) {
		this.increaseZoom();
		this.drawMap();
	}
	
	@FXML protected void zoomOutMap(ActionEvent event) {
		this.decreaseZoom();
		this.drawMap();
	}
	
    
	public void increaseZoom() {
		if (this.ZOOM < 20) {
			this.ZOOM += 1;
		} else {
			//TO DO: disable button
		}
	}
	
	public void decreaseZoom() {
		if (this.ZOOM > 0) {
			this.ZOOM -= 1;
		} else {
			
		}
	}
	
    private double[] getOffset(Tile tile) {
        double canvasCenterX = this.canvas.getWidth() / 2;
        double canvasCenterY = this.canvas.getHeight() / 2;
        
        double pixelX = MercatorProjection.longitudeToPixelX(LNG, ZOOM, this.TILE_WIDTH);
        double pixelY = MercatorProjection.latitudeToPixelY(LAT, ZOOM, this.TILE_WIDTH);
        
        double tileX = MercatorProjection.tileXToLongitude(tile.tileX, ZOOM);
        double tileY = MercatorProjection.tileYToLatitude(tile.tileY, ZOOM);
        
        tileX = MercatorProjection.longitudeToPixelX(tileX, ZOOM, this.TILE_WIDTH);
        tileY = MercatorProjection.latitudeToPixelY(tileY, ZOOM, this.TILE_WIDTH);
        
        
        double offset[] = new double[2];
        offset[1] = tileY - pixelY;
        offset[0] = tileX  - pixelX;
        return offset;
    }

    
    private void drawWithOffset(Image im, double offsetX, double offsetY) {
    	this.gc.drawImage(im, (this.canvas.getWidth() / 2) + offsetX, (this.canvas.getHeight() / 2) + offsetY);
    }
    
    private ArrayList<RendererJob> getRequiredJobs(double LAT, double LNG, byte ZOOM) {
    	int ty = MercatorProjection.latitudeToTileY(LAT, ZOOM);
        int tx = MercatorProjection.longitudeToTileX(LNG, ZOOM);
    	
    	ArrayList<RendererJob> requiredJobs = new ArrayList<RendererJob>();
    	Tile centerTile = new Tile(tx, ty, ZOOM, this.TILE_WIDTH);
    	
    	double numTilesX = (int)(this.canvas.getWidth() / 2) / this.TILE_WIDTH + 2.0;
    	double numTilesY = (int)(this.canvas.getHeight() / 2) / this.TILE_WIDTH + 2.0;
    	
    	for (int i = (int) -numTilesX; i < numTilesX; i++) {
    		for (int j = (int) -numTilesY; j < numTilesY; j++) {
    			Tile presentTile = new Tile(tx+i, ty+j, ZOOM, this.TILE_WIDTH);
    			RendererJob job = new RendererJob(presentTile, this.mapData, this.rtf, this.dm, 1.0f, false, false);
    			requiredJobs.add(job);
    		}
    	}
    	
    	return requiredJobs;
    }

    
    public void drawMap() {
        ArrayList<RendererJob> requiredJobs = this.getRequiredJobs(LAT, LNG, ZOOM);
        for (RendererJob job: requiredJobs) {
        	double[] offset = this.getOffset(job.tile);
        	double offsetX = offset[0];
        	double offsetY = offset[1];
        	
        	TileBitmap tb = renderer.executeJob(job);
        	
        	
        	ByteArrayOutputStream os = new ByteArrayOutputStream();

        	try {
        		tb.compress(os);
        		Image im = new Image(new ByteArrayInputStream(os.toByteArray()));
        		this.drawWithOffset(im, offsetX, offsetY);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.out.println("TESTSTS");
				e.printStackTrace();
			}
        }
    }


}