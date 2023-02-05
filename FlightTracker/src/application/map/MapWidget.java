package application.map;

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.awt.graphics.AwtGraphicFactory;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.layer.cache.FileSystemTileCache;
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

import application.ResizableCanvas;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * This sample demo how to render & save a tile.
 */
public class MapWidget extends StackPane {
	private ResizableCanvas canvas;
	private GraphicsContext gc;
	
	
	
	public MapWidget() {
		super();
		this.canvas = new ResizableCanvas();
		
		this.widthProperty().addListener(e -> {
			this.canvas.resize(this.getWidth(), this.getHeight());
			this.draw();
			this.drawFile();
		});
		
		this.heightProperty().addListener(e -> {
			this.canvas.resize(this.getWidth(), this.getHeight());
			this.draw();
			this.drawFile();
		});
		
		
		
		this.gc = this.canvas.getGraphicsContext2D();
		//this.getChildren().add(new Button("TEST BUTTON"));
		this.getChildren().add(canvas);
		this.draw();
		this.drawFile();
	}
	
	public void draw() {
		this.gc.setFill(Color.GREEN);
		this.gc.fillRect(0, 0, 10000, 10000);
	}
	
	

    private static final String HOME = System.getProperty("user.home");
    private static final String SAVE_PATH = "Documents/MyTiles";

    // Your compiled map. 
    private static final File DEFAULT_MAP_PATH = new File(HOME + "\\OneDrive\\University\\Undergraduate\\MRT\\GroundStation\\FlightTracker\\prince-edward-island.map");

    // Location you'd like to render.
    private static final double LAT = 46.249422;
    private static final double LNG = -63.139254;
    private static final byte ZOOM = 16;
    
    public void drawFile() {
    	MapDataStore mapData = new MapFile(DEFAULT_MAP_PATH);
    	final int ty = MercatorProjection.latitudeToTileY(LAT, ZOOM);
        final int tx = MercatorProjection.longitudeToTileX(LNG, ZOOM);
        Tile tile = new Tile(tx, ty, ZOOM, 256);

        // Create requirements.
        GraphicFactory gf = AwtGraphicFactory.INSTANCE;
        XmlRenderTheme theme = InternalRenderTheme.OSMARENDER;
        DisplayModel dm = new FixedTileSizeDisplayModel(256);
        RenderThemeFuture rtf = new RenderThemeFuture(gf, theme, dm);
        RendererJob theJob = new RendererJob(tile, mapData, rtf, dm, 1.0f, false, false);
        InMemoryTileCache tileCache = new InMemoryTileCache(256);
        
        TileBasedLabelStore tileBasedLabelStore = new TileBasedLabelStore(tileCache.getCapacityFirstLevel());
        
        // Create RendererTheme.
        Thread t = new Thread(rtf);
        t.start();

        DatabaseRenderer renderer = new DatabaseRenderer(mapData, gf, tileCache, tileBasedLabelStore, true, true, null);

        // Draw tile and save as PNG.
        TileBitmap tb = renderer.executeJob(theJob);
        tileCache.put(theJob, tb);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
			tb.compress(os);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
       
       Image im = new Image(new ByteArrayInputStream(os.toByteArray()));
       this.gc.drawImage(im, 0, 0);
    }
}