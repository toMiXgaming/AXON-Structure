package pl.axon.structure.gui;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/** Kopiuje jeden kafelek 128 × 128 z ekranu gracza na mapę. */
final class GuiMapRenderer extends MapRenderer {

    private final Function<Player, byte[]> pixels;
    private final int tile;

    GuiMapRenderer(Function<Player, byte[]> pixels, int tile) {
        super(true);
        this.pixels = pixels;
        this.tile = tile;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void render(@NotNull MapView map, @NotNull MapCanvas canvas, @NotNull Player player) {
        byte[] image = pixels.apply(player);
        if (image == null) {
            return;
        }
        int offsetX = (tile % GuiLayout.COLS) * GuiLayout.TILE;
        int offsetY = (tile / GuiLayout.COLS) * GuiLayout.TILE;
        for (int y = 0; y < GuiLayout.TILE; y++) {
            int row = (offsetY + y) * GuiLayout.WIDTH + offsetX;
            for (int x = 0; x < GuiLayout.TILE; x++) {
                canvas.setPixel(x, y, image[row + x]);
            }
        }
    }
}
