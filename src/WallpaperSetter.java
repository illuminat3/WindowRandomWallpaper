import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WallpaperSetter extends JFrame {
    private static final Logger LOGGER = Logger.getLogger(WallpaperSetter.class.getName());
    private static final String imageUrl = "http://185.124.108.230:314/get-random-image";
    private static final int TITLE_BAR_HEIGHT = 15;
    private Point initialClick;

    public WallpaperSetter() {
        setUndecorated(true); // This will remove the title bar
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(300, 220); // Increased height by 20px for the title bar
        getContentPane().setBackground(new Color(40, 40, 40)); // Set a dark gray background
        setLayout(new BorderLayout()); // Change layout to BorderLayout
        setResizable(false); // Make the window non-resizable

        // Custom title bar
        JPanel titleBar = new JPanel();
        titleBar.setPreferredSize(new Dimension(getWidth(), TITLE_BAR_HEIGHT));
        titleBar.setBackground(new Color(20,20, 20)); // Match the background color
        titleBar.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0)); // Adjust for no gaps

        // Custom close button without a background
        JLabel closeLabel = new JLabel(createIcon(Color.RED));
        closeLabel.setOpaque(false); // Ensure the label is not opaque
        closeLabel.setBorder(BorderFactory.createEmptyBorder());
        closeLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                System.exit(0);
            }
        });

        // Custom minimize button without a background
        JLabel minimizeLabel = new JLabel(createIcon(Color.ORANGE));
        minimizeLabel.setOpaque(false); // Ensure the label is not opaque
        minimizeLabel.setBorder(BorderFactory.createEmptyBorder());
        minimizeLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                setState(JFrame.ICONIFIED);
            }
        });

        // Add mouse listener to the title bar for dragging the window
        titleBar.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                initialClick = e.getPoint();
                getComponentAt(initialClick);
            }
        });

        titleBar.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                // get location of Window
                int thisX = getLocation().x;
                int thisY = getLocation().y;

                // Determine how much the mouse moved since the initial click
                int xMoved = e.getX() - initialClick.x;
                int yMoved = e.getY() - initialClick.y;

                // Move window to this position
                int X = thisX + xMoved;
                int Y = thisY + yMoved;
                setLocation(X, Y);
            }
        });

        // Add the close and minimize buttons
        closeLabel.setBorder(BorderFactory.createEmptyBorder(3, 3, 0, 0));
        titleBar.add(closeLabel);
        minimizeLabel.setBorder(BorderFactory.createEmptyBorder(3, 3, 0, 0));
        titleBar.add(minimizeLabel);

        // Content panel
        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setBackground(new Color(40, 40, 40)); // Match the background color

        JButton fetchImageButton = getjButton();

        contentPanel.add(fetchImageButton, new GridBagConstraints()); // Add button to content panel

        // Add title bar and content panel to the frame
        add(titleBar, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);

        setVisible(true);
    }

    private static JButton getjButton() {
        JButton fetchImageButton = new JButton("Get New Wallpaper");
        fetchImageButton.setBackground(new Color(140, 140, 140)); // Light gray button
        fetchImageButton.setForeground(Color.BLACK); // Text color
        fetchImageButton.setOpaque(true);
        fetchImageButton.setBorderPainted(false);
        fetchImageButton.setFocusPainted(false);

        fetchImageButton.addActionListener(e -> {
            try {
                Path imagePath = fetchAndSaveImage(imageUrl);
                setWallpaper(imagePath.toString());
            } catch (IOException | InterruptedException ex) {
                LOGGER.log(Level.SEVERE, "Failed to set wallpaper", ex);
            }
        });
        return fetchImageButton;
    }

    // Method to create icon for labels
    private Icon createIcon(Color color) {
        BufferedImage icon = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = icon.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);
        g2.fillOval(0, 0, 10, 10);
        g2.dispose();
        return new ImageIcon(icon);
    }


    public static Path fetchAndSaveImage(String url) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();

        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        Path filePath = Paths.get(System.getProperty("user.home"), "downloaded_wallpaper.jpg");

        try (InputStream inputStream = response.body()) {
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
        }

        LOGGER.info("Image saved to: " + filePath);
        return filePath;
    }

    public static void setWallpaper(String imagePath) {
        try {
            String scriptPath = "src/set_wallpaper.py";

            // Ensure you adjust "python" to the path of your Python interpreter if necessary
            String[] command = new String[]{"python", scriptPath, imagePath};

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            // Optionally, set the working directory (not always needed)
            // processBuilder.directory(new File("path_to_your_project_root"));

            Process process = processBuilder.start();

            // Wait for the script to complete
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                LOGGER.info("Wallpaper set successfully.");
            } else {
                LOGGER.severe("Python script to set wallpaper exited with error code: " + exitCode);
                // Optionally, read and log the error stream
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        LOGGER.severe(line);
                    }
                }
            }
        } catch (IOException | InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Failed to set wallpaper using Python script", e);
            // Restore interrupted status
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(WallpaperSetter::new);
    }
}
