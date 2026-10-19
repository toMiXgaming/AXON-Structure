package pl.axon.structure.oasis;

import org.bukkit.entity.Player;
import pl.axon.structure.gui.GuiCanvas;
import pl.axon.structure.gui.GuiLayout;

import java.util.List;
import java.util.Objects;

/**
 * Tablica zleceń jako ekran GUI: co przynieść, nagroda, ile już masz i przycisk oddania. Oddaje
 * {@link JobBoard#complete}, więc zasady (raz na okres, pełna ilość) są te same co w oknie skrzyni.
 */
final class JobScreen extends OasisScreen {

    private static final int LIST_X = GuiLayout.CONTENT_X;
    private static final int TOP = GuiLayout.CONTENT_Y;
    private static final int H = GuiLayout.CONTENT_H;
    private static final int LIST_W = 404;
    private static final int ROWS_Y = TOP + 38;
    private static final int VISIBLE = (TOP + H - 8 - ROWS_Y) / ROW_STEP;
    private static final int INFO_X = LIST_X + LIST_W + 8;
    private static final int INFO_W = GuiLayout.CONTENT_X + GuiLayout.CONTENT_W - INFO_X;
    private static final int COL_REWARD = 208;
    private static final int COL_HAVE = 274;

    private final JobBoard board;
    private final List<Integer> jobs;
    private int first;

    JobScreen(OasisService oasis, JobBoard board, Player player, List<Integer> jobs) {
        super(oasis, player);
        this.board = board;
        this.jobs = jobs;
    }

    @Override
    public String title() {
        return "Tablica zleceń";
    }

    @Override
    protected List<String[]> hints() {
        return List.of(new String[]{"LPM", "oddaj towar"}, new String[]{"Scroll", "lista"},
                new String[]{"Shift", "zamknij"});
    }

    @Override
    protected int state() {
        return Objects.hash(super.state(), board.untilRefresh() / 60_000L);
    }

    @Override
    protected void scroll(int delta) {
        first = clampFirst(first + delta * 2, VISIBLE, jobs.size());
    }

    @Override
    protected void openChest(Player player) {
        board.openChest(player, jobs);
    }

    @Override
    protected void draw(GuiCanvas c) {
        c.panel(LIST_X, TOP, LIST_W, H, "Zlecenia (" + jobs.size() + ")");
        int x = LIST_X + 8;
        int w = LIST_W - 22;
        c.text(x + 6, TOP + 25, "PRZYNIEŚ", GuiCanvas.TEXT_MUTED);
        c.text(x + COL_REWARD, TOP + 25, "NAGRODA", GuiCanvas.TEXT_MUTED);
        c.text(x + COL_HAVE, TOP + 25, "MASZ", GuiCanvas.TEXT_MUTED);
        c.fill(x, TOP + 34, w, 1, GuiCanvas.PANEL_LIGHT);
        first = clampFirst(first, VISIBLE, jobs.size());
        int doneCount = 0;
        for (int index : jobs) {
            if (board.done(player, index)) {
                doneCount++;
            }
        }
        for (int i = 0; i < VISIBLE && first + i < jobs.size(); i++) {
            int index = jobs.get(first + i);
            JobBoard.Job job = board.job(index);
            int y = ROWS_Y + i * ROW_STEP;
            boolean done = board.done(player, index);
            int have = Items.count(player, job.item());
            boolean hover = row(c, x, y, w, first + i, done);
            c.text(x + 6, y + 6, GuiCanvas.fit(job.amount() + "x " + name(job.item()), COL_REWARD - 12),
                    done ? GuiCanvas.TEXT_DIM : GuiCanvas.TEXT);
            c.text(x + COL_REWARD, y + 6, GuiCanvas.fit(oasis.vault().format(job.reward()), 60), GuiCanvas.GOLD);
            c.text(x + COL_HAVE, y + 6, done ? "-" : have + "/" + job.amount(),
                    done ? GuiCanvas.TEXT_MUTED : have >= job.amount() ? GuiCanvas.GREEN : GuiCanvas.TEXT_DIM);
            if (done) {
                c.right(x + w - 6, y + 6, "Wykonane", GuiCanvas.GREEN);
                c.area("zlecenie" + index, x, y, w, ROW_H, right -> {
                });
            } else {
                boolean ready = have >= job.amount();
                c.area("zlecenie" + index, x, y, w, ROW_H, right -> {
                });
                c.button("oddaj" + index, x + w - 50, y + 2, 46, 16, "Oddaj",
                        ready ? GuiCanvas.Style.PRIMARY : GuiCanvas.Style.DISABLED,
                        ready ? right -> board.complete(player, index) : null);
            }
            if (hover) {
                List<String> lines = describe(job.item());
                lines.set(0, name(job.item()));
                lines.add(1, done ? "Wykonane w tym okresie." : "Przynieś " + job.amount() + " szt., masz " + have + ".");
                c.tooltip(lines);
            }
        }
        scrollbar(c, LIST_X + LIST_W - 10, ROWS_Y, VISIBLE * ROW_STEP - 2, first, VISIBLE, jobs.size());

        c.panel(INFO_X, TOP, INFO_W, H, "Tablica");
        int ix = INFO_X + 10;
        int iw = INFO_W - 20;
        int y = TOP + 28;
        y = ShopScreen.stat(c, ix, y, iw, "Wykonane", doneCount + "/" + jobs.size(), GuiCanvas.TEXT);
        long left = board.untilRefresh();
        y = ShopScreen.stat(c, ix, y, iw, "Nowe zlecenia", "za " + (left / 3_600_000L) + " h " + (left / 60_000L % 60)
                + " min", GuiCanvas.TEXT_DIM);
        c.bar(ix, y + 2, iw, 8, jobs.isEmpty() ? 0 : doneCount / (double) jobs.size(), GuiCanvas.GREEN);
        y += 20;
        for (String line : GuiCanvas.wrap("Przynieś towar z listy, a dostaniesz nagrodę z kasy oazy. Każde zlecenie "
                + "wykonasz raz, do odświeżenia tablicy.", iw)) {
            c.text(ix, y, line, GuiCanvas.TEXT_DIM);
            y += 10;
        }
    }
}
