package application.map;

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.awt.graphics.AwtGraphicFactory;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.layer.cache.FileSystemTileCache;
import org.mapsforge.map.layer.labels.TileBasedLabelStore;
import org.mapsforge.map.layer.renderer.DatabaseRenderer;
import org.mapsforge.map.layer.renderer.RendererJob;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.model.FixedTileSizeDisplayModel;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.InternalRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderTheme;
import org.mapsforge.map.rendertheme.rule.RenderThemeFuture;

import javafx.scene.Group;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

import java.io.File;
import java.io.IOException;

/**
 * This sample demo how to render & save a tile.
 */
public class MapWidget extends StackPane {
	private Canvas canvas;
	private GraphicsContext gc;

    private static final String HOME = System.getProperty("user.home");
    private static final String SAVE_PATH = "Documents/MyTiles";

    // Your compiled map. 
    private static final File DEFAULT_MAP_PATH = new File(HOME + "\\OneDrive\\University\\Undergraduate\\MRT\\GroundStation\\FlightTracker\\prince-edward-island.map");

    // Location you'd like to render.
    private static final double LAT = 46.249422;
    private static final double LNG = -63.139254;
    private static final byte ZOOM = 16;

    public static void main(String[] args) throws IOException {
        // TODO Use args for all parameters

        // Load map.
        MapDataStore mapData = new MapFile(DEFAULT_MAP_PATH);

        // Assign tile.
        final int ty = MercatorProjection.latitudeToTileY(LAT, ZOOM);
        final int tx = MercatorProjection.longitudeToTileX(LNG, ZOOM);
        Tile tile = new Tile(tx, ty, ZOOM, 256);

        // Create requirements.
        GraphicFactory gf = AwtGraphicFactory.INSTANCE;
        XmlRenderTheme theme = InternalRenderTheme.OSMARENDER;
        DisplayModel dm = new FixedTileSizeDisplayModel(256);
        RenderThemeFuture rtf = new RenderThemeFuture(gf, theme, dm);
        RendererJob theJob = new RendererJob(tile, mapData, rtf, dm, 1.0f, false, false);
        File cacheDir = new File(HOME, SAVE_PATH);
        FileSystemTileCache tileCache = new FileSystemTileCache(10, cacheDir, gf, false);
        TileBasedLabelStore tileBasedLabelStore = new TileBasedLabelStore(tileCache.getCapacityFirstLevel());

        // Create renderer.
        DatabaseRenderer renderer = new DatabaseRenderer(mapData, gf, tileCache, tileBasedLabelStore, true, true, null);

        // Create RendererTheme.
        Thread t = new Thread(rtf);
        t.start();

        // Draw tile and save as PNG.
        TileBitmap tb = renderer.executeJob(theJob);
        tileCache.put(theJob, tb);

        // Close map.
        mapData.close();

        System.out.printf("Tile has been saved at %s/%d/%d/%d.tile.\n", cacheDir.getPath(), ZOOM, tx, ty);
    }
}