import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class ModularWallpaperLinux {
    private static String imagePathGlobal = null;

    public static void main(String[] args) {
        setSystemTrayIcon();
        startImageLoop();
    }

    private static void startImageLoop() {
        while (true) {
            createAndUpdateWallpaper();
            try {
                Thread.sleep(1000 * 10);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static void createAndUpdateWallpaper() {
        BufferedImage image = new BufferedImage(1920, 1080, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = (Graphics2D) image.getGraphics();
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(1));
        g.fillRect(0, 0, 1920, 1080);

        g.setColor(Color.WHITE);
        Path fontPath = Paths.get(getDataPath(), "/timeburnerbold.ttf");

        try {
            Font font = Font.createFont(Font.TRUETYPE_FONT, fontPath.toFile());
            font = font.deriveFont(Font.BOLD, 305);
            g.setFont(font);

            DateFormat dateFormat = new SimpleDateFormat("hh:mm");
            String timeString = dateFormat.format(new Date());

            FontMetrics fontMetrics = g.getFontMetrics(font);
            double x = 1920.0 / 2.0 - fontMetrics.stringWidth(timeString) / 2.0;
            double y = 1080.0 / 2.0 -  fontMetrics.getHeight() / 2.0;
            g.drawString(timeString, (float) x, (float) y);
        }
        catch (FontFormatException | IOException e) {
            e.printStackTrace();
        }

        if (imagePathGlobal != null)
            new File(imagePathGlobal).delete();
        imagePathGlobal = String.format("/tmp/%s.png", UUID.randomUUID());
        Path imagePath = Paths.get(imagePathGlobal);
        try {
            ImageIO.write(image, "png", imagePath.toFile());
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        updateWallpaper();
    }

    private static String getDataPath() {
        return Paths.get(System.getenv("HOME"), "/.modular_wallpaper/")
                .toAbsolutePath().toString();
    }

    private static void updateWallpaper() {
        Path imagePath = Paths.get( imagePathGlobal);
        try {
            ProcessBuilder processBuilder;
            switch (System.getenv("XDG_CURRENT_DESKTOP").toUpperCase(Locale.US)) {
                case "XFCE": processBuilder = new ProcessBuilder(
                        "xfconf-query",
                        "-c", "xfce4-desktop",
                        "-p", "/backdrop/screen0/monitoreDP-1/workspace0/last-image",
                        "-s", imagePath.toAbsolutePath().toString()
                ); break;
                case "GNOME": processBuilder = new ProcessBuilder(
                        "gsettings",
                        "set",
                        "org.gnome.desktop.background picture-uri",
                        imagePath.toAbsolutePath().toString()
                ); break;
                default: processBuilder = null;
                System.out.println("Unsupported desktop environment");
                break;
            }
            if (processBuilder != null)
                processBuilder.start();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void setSystemTrayIcon() {
        SystemTray systemTray;
        try {
            systemTray = SystemTray.getSystemTray();
        }
        catch (UnsupportedOperationException e) {
            System.out.println("The system tray is not supported in this desktop environment!");
            return;
        }
        Path imagePath = Paths.get(getDataPath(), "/icon.png");
        Image image = Toolkit.getDefaultToolkit().getImage(imagePath.toString());

        PopupMenu popupMenu = new PopupMenu();

        MenuItem exit = new MenuItem("Exit");
        exit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        popupMenu.add(exit);

        TrayIcon trayIcon = new TrayIcon(image, "Modular Wallpaper", popupMenu);

        try {
            systemTray.add(trayIcon);
        }
        catch (AWTException e) {
            e.printStackTrace();
        }

        // Hide dock icon
        System.setProperty("java.awt.headless", "true");
    }
}
