package com.example.assingment;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.scene.chart.*;
import javafx.collections.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import javafx.scene.text.Font;
import javafx.scene.paint.Color;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.Node;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import java.io.*;
import javafx.beans.binding.Bindings;


public class VehicleRentalSystem extends Application {
    private Connection connection;
    private TabPane mainTabPane;
    private BorderPane root;
    private ObservableList<Vehicle> vehicles = FXCollections.observableArrayList();
    private ObservableList<Customer> customers = FXCollections.observableArrayList();
    private ObservableList<Booking> bookings = FXCollections.observableArrayList();
    private User currentUser;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            if (!initializeDatabase()) {
                showAlert("Critical Error", "Failed to initialize database. Application will exit.");
                Platform.exit();
                return;
            }
            showWelcomeScreen(primaryStage);
        } catch (Exception e) {
            showAlert("Startup Error", "Failed to start application: " + e.getMessage());
            Platform.exit();
        }
    }

    private boolean initializeDatabase() {
        try {
            // Load MySQL driver
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Database connection parameters
            String url = "jdbc:mysql://localhost:3306/vehicle_rental";
            String username = "root";
            String password = "Mothwana21"; // Change this to your actual password

            // Create connection
            connection = DriverManager.getConnection(url, username, password);

            // Verify connection
            if (connection == null || connection.isClosed()) {
                throw new SQLException("Failed to establish database connection");
            }

            // Create tables if they don't exist
            Statement stmt = connection.createStatement();

            // Create users table
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "user_id INT PRIMARY KEY AUTO_INCREMENT, " +
                    "username VARCHAR(100) UNIQUE NOT NULL, " +
                    "password VARCHAR(100) NOT NULL, " +
                    "role VARCHAR(50) NOT NULL)");

            // Create vehicles table
            stmt.execute("CREATE TABLE IF NOT EXISTS vehicles (" +
                    "vehicle_id INT PRIMARY KEY AUTO_INCREMENT, " +
                    "brand VARCHAR(100) NOT NULL, " +
                    "model VARCHAR(100) NOT NULL, " +
                    "category VARCHAR(50) NOT NULL, " +
                    "price_per_day DECIMAL(10,2) NOT NULL, " +
                    "available BOOLEAN DEFAULT TRUE)");

            // Create customers table
            stmt.execute("CREATE TABLE IF NOT EXISTS customers (" +
                    "customer_id INT PRIMARY KEY AUTO_INCREMENT, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "phone VARCHAR(20), " +
                    "email VARCHAR(100), " +
                    "license_number VARCHAR(50))");

            // Create bookings table
            stmt.execute("CREATE TABLE IF NOT EXISTS bookings (" +
                    "booking_id INT PRIMARY KEY AUTO_INCREMENT, " +
                    "customer_id INT NOT NULL, " +
                    "vehicle_id INT NOT NULL, " +
                    "start_date DATE NOT NULL, " +
                    "end_date DATE NOT NULL, " +
                    "status VARCHAR(50) DEFAULT 'Active', " +
                    "FOREIGN KEY(customer_id) REFERENCES customers(customer_id), " +
                    "FOREIGN KEY(vehicle_id) REFERENCES vehicles(vehicle_id))");

            // Create payments table
            stmt.execute("CREATE TABLE IF NOT EXISTS payments (" +
                    "payment_id INT PRIMARY KEY AUTO_INCREMENT, " +
                    "booking_id INT NOT NULL, " +
                    "amount DECIMAL(10,2) NOT NULL, " +
                    "method VARCHAR(50) NOT NULL, " +
                    "status VARCHAR(50) DEFAULT 'Pending', " +
                    "payment_date DATE, " +
                    "FOREIGN KEY(booking_id) REFERENCES bookings(booking_id))");

            // Check if default users exist
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS count FROM users");
            if (rs.next() && rs.getInt("count") == 0) {
                // Create default admin and employee users
                stmt.execute("INSERT INTO users (username, password, role) VALUES " +
                        "('admin', 'admin123', 'Admin'), " +
                        "('employee', 'emp123', 'Employee')");
            }

            // Check if sample vehicles exist
            rs = stmt.executeQuery("SELECT COUNT(*) AS count FROM vehicles");
            if (rs.next() && rs.getInt("count") == 0) {
                // Create sample vehicles
                stmt.execute("INSERT INTO vehicles (brand, model, category, price_per_day) VALUES " +
                        "('Toyota', 'Corolla', 'Car', 50.00), " +
                        "('Honda', 'Civic', 'Car', 55.00), " +
                        "('BMW', 'R1250GS', 'Bike', 75.00)");
            }

            // Check if sample customers exist
            rs = stmt.executeQuery("SELECT COUNT(*) AS count FROM customers");
            if (rs.next() && rs.getInt("count") == 0) {
                // Create sample customers
                stmt.execute("INSERT INTO customers (name, phone, email, license_number) VALUES " +
                        "('John Smith', '555-1234', 'john@example.com', 'DL123456'), " +
                        "('Sarah Johnson', '555-5678', 'sarah@example.com', 'DL654321')");
            }

            return true;
        } catch (ClassNotFoundException e) {
            showAlert("Driver Error", "MySQL JDBC driver not found: " + e.getMessage());
            return false;
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to initialize database: " + e.getMessage());
            return false;
        } catch (Exception e) {
            showAlert("Error", "Unexpected error during database initialization: " + e.getMessage());
            return false;
        }
    }

    private void showWelcomeScreen(Stage primaryStage) {
        primaryStage.setTitle("Vehicle Rental System");

        VBox welcomePane = new VBox(20);
        welcomePane.setAlignment(Pos.CENTER);
        welcomePane.setPadding(new Insets(50));

        Label titleLabel = new Label("Welcome to Vehicle Rental System");
        titleLabel.setFont(Font.font(24));
        titleLabel.setTextFill(Color.DARKBLUE);

        Button loginButton = new Button("Login");
        loginButton.setPrefWidth(200);
        loginButton.setStyle("-fx-font-size: 16px;");

        Button registerButton = new Button("Register");
        registerButton.setPrefWidth(200);
        registerButton.setStyle("-fx-font-size: 16px;");

        loginButton.setOnAction(e -> showLoginScreen(primaryStage));
        registerButton.setOnAction(e -> showRegisterScreen(primaryStage));

        welcomePane.getChildren().addAll(titleLabel, loginButton, registerButton);

        Scene scene = new Scene(welcomePane, 500, 400);

// Apply CSS from the resources/styles folder
        scene.getStylesheets().add(getClass().getResource("/Style.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.show();


    }

    private void showLoginScreen(Stage primaryStage) {
        primaryStage.setTitle("Vehicle Rental System - Login");

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25, 25, 25, 25));

        Label titleLabel = new Label("Login to Your Account");
        titleLabel.setFont(Font.font(20));
        grid.add(titleLabel, 0, 0, 2, 1);

        Label usernameLabel = new Label("Username:");
        grid.add(usernameLabel, 0, 1);

        TextField usernameField = new TextField();
        usernameField.setPromptText("Enter your username");
        grid.add(usernameField, 1, 1);

        Label passwordLabel = new Label("Password:");
        grid.add(passwordLabel, 0, 2);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter your password");
        grid.add(passwordField, 1, 2);

        Button loginButton = new Button("Login");
        Button registerButton = new Button("Don't have an account? Register");

        HBox hbBtn = new HBox(10);
        hbBtn.setAlignment(Pos.BOTTOM_RIGHT);
        hbBtn.getChildren().addAll(registerButton, loginButton);
        grid.add(hbBtn, 1, 4);

        final Label messageLabel = new Label();
        grid.add(messageLabel, 1, 6);

        loginButton.setOnAction(e -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText().trim();

            if (username.isEmpty() || password.isEmpty()) {
                messageLabel.setText("Please enter both username and password");
                messageLabel.setTextFill(Color.RED);
                return;
            }

            try {
                if (authenticateUser(username, password)) {
                    loadDataFromDatabase();
                    createMainUI(primaryStage);
                } else {
                    messageLabel.setText("Invalid username or password");
                    messageLabel.setTextFill(Color.RED);
                }
            } catch (Exception ex) {
                messageLabel.setText("Login failed. Please try again.");
                messageLabel.setTextFill(Color.RED);
                ex.printStackTrace();
            }
        });

        registerButton.setOnAction(e -> showRegisterScreen(primaryStage));

        Scene scene = new Scene(grid, 500, 350);

// Add CSS from resources folder
        scene.getStylesheets().add(getClass().getResource("/Style.css").toExternalForm());

        primaryStage.setScene(scene);

    }

    private boolean authenticateUser(String username, String password) throws SQLException {
        String query = "SELECT user_id, username, role FROM users WHERE username = ? AND password = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, username);
            statement.setString(2, password);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    currentUser = new User(
                            rs.getInt("user_id"),
                            rs.getString("username"),
                            rs.getString("role")
                    );
                    return true;
                }
            }
        }
        return false;
    }

    private void showRegisterScreen(Stage primaryStage) {
        primaryStage.setTitle("Vehicle Rental System - Register");

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25, 25, 25, 25));

        Label titleLabel = new Label("Create New Account");
        titleLabel.setFont(Font.font(20));
        grid.add(titleLabel, 0, 0, 2, 1);

        Label usernameLabel = new Label("Username:");
        grid.add(usernameLabel, 0, 1);

        TextField usernameField = new TextField();
        usernameField.setPromptText("Choose a username");
        grid.add(usernameField, 1, 1);

        Label passwordLabel = new Label("Password:");
        grid.add(passwordLabel, 0, 2);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Create a password");
        grid.add(passwordField, 1, 2);

        Label confirmPasswordLabel = new Label("Confirm Password:");
        grid.add(confirmPasswordLabel, 0, 3);

        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Re-enter your password");
        grid.add(confirmPasswordField, 1, 3);

        Label roleLabel = new Label("Account Type:");
        grid.add(roleLabel, 0, 4);

        ComboBox<String> roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll("Employee", "Admin");
        roleCombo.setValue("Employee");
        grid.add(roleCombo, 1, 4);

        Button registerButton = new Button("Register");
        Button backButton = new Button("Back to Login");

        HBox hbBtn = new HBox(10);
        hbBtn.setAlignment(Pos.BOTTOM_RIGHT);
        hbBtn.getChildren().addAll(backButton, registerButton);
        grid.add(hbBtn, 1, 5);

        final Label messageLabel = new Label();
        grid.add(messageLabel, 1, 6);

        registerButton.setOnAction(e -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText().trim();
            String confirmPassword = confirmPasswordField.getText().trim();
            String role = roleCombo.getValue();

            if (username.isEmpty() || password.isEmpty()) {
                messageLabel.setText("Username and password are required");
                messageLabel.setTextFill(Color.RED);
                return;
            }

            if (username.length() < 4) {
                messageLabel.setText("Username must be at least 4 characters");
                messageLabel.setTextFill(Color.RED);
                return;
            }

            if (password.length() < 6) {
                messageLabel.setText("Password must be at least 6 characters");
                messageLabel.setTextFill(Color.RED);
                return;
            }

            if (!password.equals(confirmPassword)) {
                messageLabel.setText("Passwords do not match");
                messageLabel.setTextFill(Color.RED);
                return;
            }

            try {
                if (registerUser(username, password, role)) {
                    messageLabel.setText("Registration successful! Please login.");
                    messageLabel.setTextFill(Color.GREEN);
                } else {
                    messageLabel.setText("Username already exists");
                    messageLabel.setTextFill(Color.RED);
                }
            } catch (Exception ex) {
                messageLabel.setText("Registration failed. Please try again.");
                messageLabel.setTextFill(Color.RED);
                ex.printStackTrace();
            }
        });

        backButton.setOnAction(e -> showLoginScreen(primaryStage));
        Scene scene = new Scene(grid, 500, 400);
        scene.getStylesheets().add(getClass().getResource("/Style.css").toExternalForm()); // Adjust path if needed
        primaryStage.setScene(scene);

    }

    private boolean registerUser(String username, String password, String role) throws SQLException {
        // First check if username already exists
        String checkQuery = "SELECT COUNT(*) AS count FROM users WHERE username = ?";
        try (PreparedStatement checkStmt = connection.prepareStatement(checkQuery)) {
            checkStmt.setString(1, username);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next() && rs.getInt("count") > 0) {
                    return false;
                }
            }
        }

        // Insert new user
        String insertQuery = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";
        try (PreparedStatement insertStmt = connection.prepareStatement(insertQuery)) {
            insertStmt.setString(1, username);
            insertStmt.setString(2, password);
            insertStmt.setString(3, role);
            return insertStmt.executeUpdate() > 0;
        }
    }

    private void loadDataFromDatabase() {
        try {
            // Load vehicles
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT vehicle_id, brand, model, category, price_per_day, available FROM vehicles")) {
                vehicles.clear();
                while (rs.next()) {
                    vehicles.add(new Vehicle(
                            rs.getInt("vehicle_id"),
                            rs.getString("brand"),
                            rs.getString("model"),
                            rs.getString("category"),
                            rs.getDouble("price_per_day"),
                            rs.getBoolean("available")
                    ));
                }
            }

            // Load customers
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT customer_id, name, phone, email, license_number FROM customers")) {
                customers.clear();
                while (rs.next()) {
                    customers.add(new Customer(
                            rs.getInt("customer_id"),
                            rs.getString("name"),
                            rs.getString("phone"),
                            rs.getString("email"),
                            rs.getString("license_number")
                    ));
                }
            }

            // Load bookings with associated vehicles and customers
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT b.booking_id, b.start_date, b.end_date, b.status, " +
                         "v.vehicle_id, v.brand, v.model, v.category, v.price_per_day, v.available, " +
                         "c.customer_id, c.name, c.phone, c.email, c.license_number " +
                         "FROM bookings b " +
                         "JOIN vehicles v ON b.vehicle_id = v.vehicle_id " +
                         "JOIN customers c ON b.customer_id = c.customer_id")) {
                bookings.clear();
                while (rs.next()) {
                    Vehicle vehicle = new Vehicle(
                            rs.getInt("vehicle_id"),
                            rs.getString("brand"),
                            rs.getString("model"),
                            rs.getString("category"),
                            rs.getDouble("price_per_day"),
                            rs.getBoolean("available")
                    );

                    Customer customer = new Customer(
                            rs.getInt("customer_id"),
                            rs.getString("name"),
                            rs.getString("phone"),
                            rs.getString("email"),
                            rs.getString("license_number")
                    );

                    bookings.add(new Booking(
                            rs.getInt("booking_id"),
                            customer,
                            vehicle,
                            rs.getDate("start_date").toLocalDate(),
                            rs.getDate("end_date").toLocalDate(),
                            rs.getString("status")
                    ));
                }
            }
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to load data: " + e.getMessage());
        }
    }

    private void createMainUI(Stage primaryStage) {
        primaryStage.setTitle("Vehicle Rental System - " + currentUser.getRole());

        root = new BorderPane();
        mainTabPane = new TabPane();

        // Create tabs based on user role
        Tab dashboardTab = new Tab("Dashboard", createDashboard());
        Tab vehiclesTab = new Tab("Vehicle Management", createVehicleManagementTab());
        Tab customersTab = new Tab("Customer Management", createCustomerManagementTab());
        Tab bookingsTab = new Tab("Booking Management", createBookingSystemTab());

        mainTabPane.getTabs().addAll(dashboardTab, vehiclesTab, customersTab, bookingsTab);

        if (currentUser.getRole().equals("Admin")) {
            Tab paymentsTab = new Tab("Payment & Billing", createPaymentBillingTab());
            Tab reportsTab = new Tab("Reports", createReportsTab());
            mainTabPane.getTabs().addAll(paymentsTab, reportsTab);
        }

        root.setCenter(mainTabPane);

        // Add logout button to top right
        HBox topBar = new HBox();
        topBar.setAlignment(Pos.CENTER_RIGHT);
        topBar.setPadding(new Insets(10));
        topBar.setSpacing(10);

        Label userLabel = new Label("Logged in as: " + currentUser.getUsername() + " (" + currentUser.getRole() + ")");
        Button logoutButton = new Button("Logout");

        logoutButton.setOnAction(e -> {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            currentUser = null;
            showWelcomeScreen(primaryStage);
        });

        topBar.getChildren().addAll(userLabel, logoutButton);
        root.setTop(topBar);

        Scene scene = new Scene(root, 1200, 800);

// Add this line to load your CSS
        scene.getStylesheets().add(getClass().getResource("/Style.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.show();

    }

    private Node createDashboard() {
        VBox dashboard = new VBox(20);
        dashboard.setPadding(new Insets(20));

        // Summary statistics
        HBox statsBox = new HBox(20);
        statsBox.setAlignment(Pos.TOP_LEFT);

        // Available vehicles card
        VBox availableVehiclesCard = createStatCard("Available Vehicles",
                String.valueOf(vehicles.stream().filter(Vehicle::isAvailable).count()),
                Color.LIGHTGREEN);

        // Total customers card
        VBox customersCard = createStatCard("Total Customers",
                String.valueOf(customers.size()),
                Color.LIGHTBLUE);

        // Active bookings card
        VBox activeBookingsCard = createStatCard("Active Bookings",
                String.valueOf(bookings.stream().filter(b -> b.getStatus().equals("Active")).count()),
                Color.LIGHTCORAL);

        statsBox.getChildren().addAll(availableVehiclesCard, customersCard, activeBookingsCard);

        // Recent bookings table
        Label recentBookingsLabel = new Label("Recent Bookings");
        recentBookingsLabel.setFont(Font.font(18));

        TableView<Booking> recentBookingsTable = new TableView<>();
        recentBookingsTable.setItems(FXCollections.observableArrayList(
                bookings.subList(0, Math.min(5, bookings.size()))
        ));

        TableColumn<Booking, String> customerCol = new TableColumn<>("Customer");
        customerCol.setCellValueFactory(cell -> cell.getValue().getCustomer().nameProperty());

        TableColumn<Booking, String> phoneCol = new TableColumn<>("Phone");
        phoneCol.setCellValueFactory(cell -> cell.getValue().getCustomer().phoneProperty());

        TableColumn<Booking, String> vehicleCol = new TableColumn<>("Vehicle");
        vehicleCol.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getVehicle().getBrand() + " " + cell.getValue().getVehicle().getModel()));

        TableColumn<Booking, LocalDate> startDateCol = new TableColumn<>("Start Date");
        startDateCol.setCellValueFactory(new PropertyValueFactory<>("startDate"));

        TableColumn<Booking, LocalDate> endDateCol = new TableColumn<>("End Date");
        endDateCol.setCellValueFactory(new PropertyValueFactory<>("endDate"));

        recentBookingsTable.getColumns().addAll(customerCol, phoneCol, vehicleCol, startDateCol, endDateCol);
        recentBookingsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Add export button for recent bookings
        Button exportRecentBookingsBtn = new Button("Export Recent Bookings to CSV");
        exportRecentBookingsBtn.setOnAction(e -> exportRecentBookingsToCSV(recentBookingsTable.getItems()));

        dashboard.getChildren().addAll(statsBox, recentBookingsLabel, recentBookingsTable, exportRecentBookingsBtn);
        return dashboard;
    }

    private VBox createStatCard(String title, String value, Color color) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: " + toRGBCode(color) + "; -fx-background-radius: 10;");

        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font(14));

        Label valueLabel = new Label(value);
        valueLabel.setFont(Font.font("System", FontWeight.BOLD, 24));

        card.getChildren().addAll(titleLabel, valueLabel);
        return card;
    }

    private String toRGBCode(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }

    private Node createVehicleManagementTab() {
        VBox vehicleManagement = new VBox(20);
        vehicleManagement.setPadding(new Insets(20));

        // Add vehicle button and export button
        HBox buttonBox = new HBox(10);
        Button addVehicleButton = new Button("Add New Vehicle");
        Button exportVehiclesButton = new Button("Export to CSV");
        buttonBox.getChildren().addAll(addVehicleButton, exportVehiclesButton);

        addVehicleButton.setOnAction(e -> showAddVehicleDialog());
        exportVehiclesButton.setOnAction(e -> exportVehiclesToCSV());

        // Vehicle table
        TableView<Vehicle> vehicleTable = new TableView<>();
        vehicleTable.setItems(vehicles);

        TableColumn<Vehicle, String> brandCol = new TableColumn<>("Brand");
        brandCol.setCellValueFactory(new PropertyValueFactory<>("brand"));

        TableColumn<Vehicle, String> modelCol = new TableColumn<>("Model");
        modelCol.setCellValueFactory(new PropertyValueFactory<>("model"));

        TableColumn<Vehicle, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));

        TableColumn<Vehicle, Double> priceCol = new TableColumn<>("Price/Day");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("pricePerDay"));
        priceCol.setCellFactory(col -> new TableCell<Vehicle, Double>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    setText(String.format("$%.2f", price));
                }
            }
        });

        TableColumn<Vehicle, Boolean> availableCol = new TableColumn<>("Available");
        availableCol.setCellValueFactory(new PropertyValueFactory<>("available"));
        availableCol.setCellFactory(col -> new TableCell<Vehicle, Boolean>() {
            @Override
            protected void updateItem(Boolean available, boolean empty) {
                super.updateItem(available, empty);
                if (empty || available == null) {
                    setText(null);
                } else {
                    setText(available ? "Yes" : "No");
                    setTextFill(available ? Color.GREEN : Color.RED);
                }
            }
        });

        vehicleTable.getColumns().addAll(brandCol, modelCol, categoryCol, priceCol, availableCol);
        vehicleTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Add context menu for editing/deleting
        ContextMenu contextMenu = new ContextMenu();
        MenuItem editItem = new MenuItem("Edit");
        MenuItem deleteItem = new MenuItem("Delete");
        contextMenu.getItems().addAll(editItem, deleteItem);

        // Enable context menu actions only when an item is selected
        vehicleTable.setRowFactory(tv -> {
            TableRow<Vehicle> row = new TableRow<>();
            row.contextMenuProperty().bind(
                    Bindings.when(row.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(contextMenu)
            );
            return row;
        });

        // Edit action
        editItem.setOnAction(e -> {
            Vehicle selected = vehicleTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showEditVehicleDialog(selected);
            }
        });

        // Delete action
        deleteItem.setOnAction(e -> {
            Vehicle selected = vehicleTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                deleteVehicle(selected);
            }
        });

        vehicleManagement.getChildren().addAll(buttonBox, vehicleTable);
        return vehicleManagement;
    }


    private void exportVehiclesToCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Vehicles Data");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName("vehicles_data.csv");

        File file = fileChooser.showSaveDialog(root.getScene().getWindow());
        if (file != null) {
            try (PrintWriter writer = new PrintWriter(file)) {
                // Write CSV header
                writer.println("ID,Brand,Model,Category,Price Per Day,Available");

                // Write data
                for (Vehicle vehicle : vehicles) {
                    writer.println(
                            vehicle.getId() + "," +
                                    escapeCsv(vehicle.getBrand()) + "," +
                                    escapeCsv(vehicle.getModel()) + "," +
                                    escapeCsv(vehicle.getCategory()) + "," +
                                    vehicle.getPricePerDay() + "," +
                                    (vehicle.isAvailable() ? "Yes" : "No")
                    );
                }

                showAlert("Success", "Vehicles data exported successfully to " + file.getName());
            } catch (FileNotFoundException e) {
                showAlert("Error", "Failed to export vehicles data: " + e.getMessage());
            }
        }
    }

    private void exportRecentBookingsToCSV(List<Booking> bookings) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Recent Bookings Data");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName("recent_bookings.csv");

        File file = fileChooser.showSaveDialog(root.getScene().getWindow());
        if (file != null) {
            try (PrintWriter writer = new PrintWriter(file)) {
                // Write CSV header
                writer.println("ID,Customer,Phone,Vehicle,Start Date,End Date");

                // Write data
                for (Booking booking : bookings) {
                    writer.println(
                            booking.getId() + "," +
                                    escapeCsv(booking.getCustomer().getName()) + "," +
                                    escapeCsv(booking.getCustomer().getPhone()) + "," +
                                    escapeCsv(booking.getVehicle().getBrand() + " " + booking.getVehicle().getModel()) + "," +
                                    booking.getStartDate() + "," +
                                    booking.getEndDate()
                    );
                }

                showAlert("Success", "Recent bookings exported successfully to " + file.getName());
            } catch (FileNotFoundException e) {
                showAlert("Error", "Failed to export recent bookings: " + e.getMessage());
            }
        }
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            escaped = "\"" + escaped + "\"";
        }
        return escaped;
    }

    private void showAddVehicleDialog() {
        Dialog<Vehicle> dialog = new Dialog<>();
        dialog.setTitle("Add New Vehicle");

        // Set up form
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField brandField = new TextField();
        brandField.setPromptText("Brand");
        TextField modelField = new TextField();
        modelField.setPromptText("Model");
        ComboBox<String> categoryCombo = new ComboBox<>();
        categoryCombo.getItems().addAll("Car", "Bike", "Truck", "SUV");
        categoryCombo.setValue("Car");
        TextField priceField = new TextField();
        priceField.setPromptText("Price per day");

        grid.add(new Label("Brand:"), 0, 0);
        grid.add(brandField, 1, 0);
        grid.add(new Label("Model:"), 0, 1);
        grid.add(modelField, 1, 1);
        grid.add(new Label("Category:"), 0, 2);
        grid.add(categoryCombo, 1, 2);
        grid.add(new Label("Price/Day:"), 0, 3);
        grid.add(priceField, 1, 3);

        dialog.getDialogPane().setContent(grid);

        // Add buttons
        ButtonType addButton = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButton, ButtonType.CANCEL);

        // Convert result to Vehicle when Add button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButton) {
                try {
                    double price = Double.parseDouble(priceField.getText());
                    return new Vehicle(0, brandField.getText(), modelField.getText(),
                            categoryCombo.getValue(), price, true);
                } catch (NumberFormatException e) {
                    showAlert("Invalid Input", "Please enter a valid price");
                    return null;
                }
            }
            return null;
        });

        Optional<Vehicle> result = dialog.showAndWait();
        result.ifPresent(vehicle -> {
            try {
                if (addVehicleToDatabase(vehicle)) {
                    vehicles.add(vehicle);
                    showAlert("Success", "Vehicle added successfully");
                } else {
                    showAlert("Error", "Failed to add vehicle");
                }
            } catch (SQLException ex) {
                showAlert("Database Error", "Failed to add vehicle: " + ex.getMessage());
            }
        });
    }

    private boolean addVehicleToDatabase(Vehicle vehicle) throws SQLException {
        String query = "INSERT INTO vehicles (brand, model, category, price_per_day, available) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, vehicle.getBrand());
            stmt.setString(2, vehicle.getModel());
            stmt.setString(3, vehicle.getCategory());
            stmt.setDouble(4, vehicle.getPricePerDay());
            stmt.setBoolean(5, vehicle.isAvailable());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        vehicle.setId(rs.getInt(1));
                        return true;
                    }
                }
            }
            return false;
        }
    }

    private void showEditVehicleDialog(Vehicle vehicle) {
        Dialog<Vehicle> dialog = new Dialog<>();
        dialog.setTitle("Edit Vehicle");

        // Set up form
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField brandField = new TextField(vehicle.getBrand());
        TextField modelField = new TextField(vehicle.getModel());
        ComboBox<String> categoryCombo = new ComboBox<>();
        categoryCombo.getItems().addAll("Car", "Bike", "Truck", "SUV");
        categoryCombo.setValue(vehicle.getCategory());
        TextField priceField = new TextField(String.valueOf(vehicle.getPricePerDay()));
        CheckBox availableCheck = new CheckBox("Available");
        availableCheck.setSelected(vehicle.isAvailable());

        grid.add(new Label("Brand:"), 0, 0);
        grid.add(brandField, 1, 0);
        grid.add(new Label("Model:"), 0, 1);
        grid.add(modelField, 1, 1);
        grid.add(new Label("Category:"), 0, 2);
        grid.add(categoryCombo, 1, 2);
        grid.add(new Label("Price/Day:"), 0, 3);
        grid.add(priceField, 1, 3);
        grid.add(availableCheck, 0, 4, 2, 1);

        dialog.getDialogPane().setContent(grid);

        // Add buttons
        ButtonType saveButton = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButton, ButtonType.CANCEL);

        // Convert result to Vehicle when Save button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButton) {
                try {
                    double price = Double.parseDouble(priceField.getText());
                    vehicle.setBrand(brandField.getText());
                    vehicle.setModel(modelField.getText());
                    vehicle.setCategory(categoryCombo.getValue());
                    vehicle.setPricePerDay(price);
                    vehicle.setAvailable(availableCheck.isSelected());
                    return vehicle;
                } catch (NumberFormatException e) {
                    showAlert("Invalid Input", "Please enter a valid price");
                    return null;
                }
            }
            return null;
        });

        Optional<Vehicle> result = dialog.showAndWait();
        result.ifPresent(updatedVehicle -> {
            try {
                if (updateVehicleInDatabase(updatedVehicle)) {
                    showAlert("Success", "Vehicle updated successfully");
                } else {
                    showAlert("Error", "Failed to update vehicle");
                }
            } catch (SQLException ex) {
                showAlert("Database Error", "Failed to update vehicle: " + ex.getMessage());
            }
        });
    }

    private boolean updateVehicleInDatabase(Vehicle vehicle) throws SQLException {
        String query = "UPDATE vehicles SET brand = ?, model = ?, category = ?, price_per_day = ?, available = ? WHERE vehicle_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, vehicle.getBrand());
            stmt.setString(2, vehicle.getModel());
            stmt.setString(3, vehicle.getCategory());
            stmt.setDouble(4, vehicle.getPricePerDay());
            stmt.setBoolean(5, vehicle.isAvailable());
            stmt.setInt(6, vehicle.getId());

            return stmt.executeUpdate() > 0;
        }
    }

    private void deleteVehicle(Vehicle vehicle) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Deletion");
        alert.setHeaderText("Delete Vehicle");
        alert.setContentText("Are you sure you want to delete this vehicle?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                String query = "DELETE FROM vehicles WHERE vehicle_id = ?";
                try (PreparedStatement stmt = connection.prepareStatement(query)) {
                    stmt.setInt(1, vehicle.getId());

                    if (stmt.executeUpdate() > 0) {
                        vehicles.remove(vehicle);
                        showAlert("Success", "Vehicle deleted successfully");
                    } else {
                        showAlert("Error", "Failed to delete vehicle");
                    }
                }
            } catch (SQLException e) {
                showAlert("Database Error", "Failed to delete vehicle: " + e.getMessage());
            }
        }
    }

    private Node createCustomerManagementTab() {
        VBox customerManagement = new VBox(20);
        customerManagement.setPadding(new Insets(20));

        // Add customer button and export button
        HBox buttonBox = new HBox(10);
        Button addCustomerButton = new Button("Add New Customer");
        Button exportCustomersButton = new Button("Export to CSV");
        buttonBox.getChildren().addAll(addCustomerButton, exportCustomersButton);

        addCustomerButton.setOnAction(e -> showAddCustomerDialog());
        exportCustomersButton.setOnAction(e -> exportCustomersToCSV());

        // Customer table
        TableView<Customer> customerTable = new TableView<>();
        customerTable.setItems(customers);

        TableColumn<Customer, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Customer, String> phoneCol = new TableColumn<>("Phone");
        phoneCol.setCellValueFactory(new PropertyValueFactory<>("phone"));

        TableColumn<Customer, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));

        TableColumn<Customer, String> licenseCol = new TableColumn<>("License");
        licenseCol.setCellValueFactory(new PropertyValueFactory<>("licenseNumber"));

        customerTable.getColumns().addAll(nameCol, phoneCol, emailCol, licenseCol);
        customerTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Add context menu for editing/deleting
        ContextMenu contextMenu = new ContextMenu();
        MenuItem editItem = new MenuItem("Edit");
        MenuItem deleteItem = new MenuItem("Delete");
        contextMenu.getItems().addAll(editItem, deleteItem);

        editItem.setOnAction(e -> {
            Customer selected = customerTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showEditCustomerDialog(selected);
            }
        });

        deleteItem.setOnAction(e -> {
            Customer selected = customerTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                deleteCustomer(selected);
            }
        });

        customerTable.setContextMenu(contextMenu);

        customerManagement.getChildren().addAll(buttonBox, customerTable);
        return customerManagement;
    }

    private void exportCustomersToCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Customers Data");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName("customers_data.csv");

        File file = fileChooser.showSaveDialog(root.getScene().getWindow());
        if (file != null) {
            try (PrintWriter writer = new PrintWriter(file)) {
                // Write CSV header
                writer.println("ID,Name,Phone,Email,License Number");

                // Write data
                for (Customer customer : customers) {
                    writer.println(
                            customer.getId() + "," +
                                    escapeCsv(customer.getName()) + "," +
                                    escapeCsv(customer.getPhone()) + "," +
                                    escapeCsv(customer.getEmail()) + "," +
                                    escapeCsv(customer.getLicenseNumber())
                    );
                }

                showAlert("Success", "Customers data exported successfully to " + file.getName());
            } catch (FileNotFoundException e) {
                showAlert("Error", "Failed to export customers data: " + e.getMessage());
            }
        }
    }

    private void showAddCustomerDialog() {
        Dialog<Customer> dialog = new Dialog<>();
        dialog.setTitle("Add New Customer");

        // Set up form
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField();
        nameField.setPromptText("Full Name");
        TextField phoneField = new TextField();
        phoneField.setPromptText("Phone Number");
        TextField emailField = new TextField();
        emailField.setPromptText("Email Address");
        TextField licenseField = new TextField();
        licenseField.setPromptText("Driver's License Number");

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Phone:"), 0, 1);
        grid.add(phoneField, 1, 1);
        grid.add(new Label("Email:"), 0, 2);
        grid.add(emailField, 1, 2);
        grid.add(new Label("License:"), 0, 3);
        grid.add(licenseField, 1, 3);

        dialog.getDialogPane().setContent(grid);

        // Add buttons
        ButtonType addButton = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButton, ButtonType.CANCEL);

        // Convert result to Customer when Add button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButton) {
                return new Customer(0, nameField.getText(), phoneField.getText(),
                        emailField.getText(), licenseField.getText());
            }
            return null;
        });

        Optional<Customer> result = dialog.showAndWait();
        result.ifPresent(customer -> {
            try {
                if (addCustomerToDatabase(customer)) {
                    customers.add(customer);
                    showAlert("Success", "Customer added successfully");
                } else {
                    showAlert("Error", "Failed to add customer");
                }
            } catch (SQLException ex) {
                showAlert("Database Error", "Failed to add customer: " + ex.getMessage());
            }
        });
    }

    private boolean addCustomerToDatabase(Customer customer) throws SQLException {
        String query = "INSERT INTO customers (name, phone, email, license_number) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, customer.getName());
            stmt.setString(2, customer.getPhone());
            stmt.setString(3, customer.getEmail());
            stmt.setString(4, customer.getLicenseNumber());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        customer.setId(rs.getInt(1));
                        return true;
                    }
                }
            }
            return false;
        }
    }

    private void showEditCustomerDialog(Customer customer) {
        Dialog<Customer> dialog = new Dialog<>();
        dialog.setTitle("Edit Customer");

        // Set up form
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField(customer.getName());
        TextField phoneField = new TextField(customer.getPhone());
        TextField emailField = new TextField(customer.getEmail());
        TextField licenseField = new TextField(customer.getLicenseNumber());

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Phone:"), 0, 1);
        grid.add(phoneField, 1, 1);
        grid.add(new Label("Email:"), 0, 2);
        grid.add(emailField, 1, 2);
        grid.add(new Label("License:"), 0, 3);
        grid.add(licenseField, 1, 3);

        dialog.getDialogPane().setContent(grid);

        // Add buttons
        ButtonType saveButton = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButton, ButtonType.CANCEL);

        // Convert result to Customer when Save button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButton) {
                customer.setName(nameField.getText());
                customer.setPhone(phoneField.getText());
                customer.setEmail(emailField.getText());
                customer.setLicenseNumber(licenseField.getText());
                return customer;
            }
            return null;
        });

        Optional<Customer> result = dialog.showAndWait();
        result.ifPresent(updatedCustomer -> {
            try {
                if (updateCustomerInDatabase(updatedCustomer)) {
                    showAlert("Success", "Customer updated successfully");
                } else {
                    showAlert("Error", "Failed to update customer");
                }
            } catch (SQLException ex) {
                showAlert("Database Error", "Failed to update customer: " + ex.getMessage());
            }
        });
    }

    private boolean updateCustomerInDatabase(Customer customer) throws SQLException {
        String query = "UPDATE customers SET name = ?, phone = ?, email = ?, license_number = ? WHERE customer_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, customer.getName());
            stmt.setString(2, customer.getPhone());
            stmt.setString(3, customer.getEmail());
            stmt.setString(4, customer.getLicenseNumber());
            stmt.setInt(5, customer.getId());

            return stmt.executeUpdate() > 0;
        }
    }

    private void deleteCustomer(Customer customer) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Deletion");
        alert.setHeaderText("Delete Customer");
        alert.setContentText("Are you sure you want to delete this customer?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                String query = "DELETE FROM customers WHERE customer_id = ?";
                try (PreparedStatement stmt = connection.prepareStatement(query)) {
                    stmt.setInt(1, customer.getId());

                    if (stmt.executeUpdate() > 0) {
                        customers.remove(customer);
                        showAlert("Success", "Customer deleted successfully");
                    } else {
                        showAlert("Error", "Failed to delete customer");
                    }
                }
            } catch (SQLException e) {
                showAlert("Database Error", "Failed to delete customer: " + e.getMessage());
            }
        }
    }

    private Node createBookingSystemTab() {
        VBox bookingSystem = new VBox(20);
        bookingSystem.setPadding(new Insets(20));

        // Create booking button and export button
        HBox buttonBox = new HBox(10);
        Button createBookingButton = new Button("Create New Booking");
        Button exportBookingsButton = new Button("Export to CSV");
        buttonBox.getChildren().addAll(createBookingButton, exportBookingsButton);

        createBookingButton.setOnAction(e -> showCreateBookingDialog());
        exportBookingsButton.setOnAction(e -> exportBookingsToCSV());

        // Booking table
        TableView<Booking> bookingTable = new TableView<>();
        bookingTable.setItems(bookings);

        TableColumn<Booking, String> customerCol = new TableColumn<>("Customer");
        customerCol.setCellValueFactory(cell -> cell.getValue().getCustomer().nameProperty());

        TableColumn<Booking, String> phoneCol = new TableColumn<>("Phone");
        phoneCol.setCellValueFactory(cell -> cell.getValue().getCustomer().phoneProperty());

        TableColumn<Booking, String> vehicleCol = new TableColumn<>("Vehicle");
        vehicleCol.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getVehicle().getBrand() + " " + cell.getValue().getVehicle().getModel()));

        TableColumn<Booking, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getVehicle().getCategory()));

        TableColumn<Booking, String> priceCol = new TableColumn<>("Price/Day");
        priceCol.setCellValueFactory(cell -> new SimpleStringProperty(
                String.format("$%.2f", cell.getValue().getVehicle().getPricePerDay())));

        TableColumn<Booking, LocalDate> startDateCol = new TableColumn<>("Start Date");
        startDateCol.setCellValueFactory(new PropertyValueFactory<>("startDate"));

        TableColumn<Booking, LocalDate> endDateCol = new TableColumn<>("End Date");
        endDateCol.setCellValueFactory(new PropertyValueFactory<>("endDate"));

        TableColumn<Booking, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setCellFactory(col -> new TableCell<Booking, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                } else {
                    setText(status);
                    if (status.equals("Active")) {
                        setTextFill(Color.GREEN);
                    } else if (status.equals("Completed")) {
                        setTextFill(Color.BLUE);
                    } else if (status.equals("Cancelled")) {
                        setTextFill(Color.RED);
                    }
                }
            }
        });

        bookingTable.getColumns().addAll(customerCol, phoneCol, vehicleCol, categoryCol, priceCol,
                startDateCol, endDateCol, statusCol);
        bookingTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Add context menu for managing bookings
        ContextMenu contextMenu = new ContextMenu();
        MenuItem cancelItem = new MenuItem("Cancel Booking");
        MenuItem completeItem = new MenuItem("Complete Booking");
        contextMenu.getItems().addAll(cancelItem, completeItem);

        cancelItem.setOnAction(e -> {
            Booking selected = bookingTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                cancelBooking(selected);
            }
        });

        completeItem.setOnAction(e -> {
            Booking selected = bookingTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                completeBooking(selected);
            }
        });

        bookingTable.setContextMenu(contextMenu);

        bookingSystem.getChildren().addAll(buttonBox, bookingTable);
        return bookingSystem;
    }

    private void exportBookingsToCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Bookings Data");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName("bookings_data.csv");

        File file = fileChooser.showSaveDialog(root.getScene().getWindow());
        if (file != null) {
            try (PrintWriter writer = new PrintWriter(file)) {
                // Write CSV header
                writer.println("ID,Customer,Phone,Vehicle,Category,Price/Day,Start Date,End Date,Status");

                // Write data
                for (Booking booking : bookings) {
                    writer.println(
                            booking.getId() + "," +
                                    escapeCsv(booking.getCustomer().getName()) + "," +
                                    escapeCsv(booking.getCustomer().getPhone()) + "," +
                                    escapeCsv(booking.getVehicle().getBrand() + " " + booking.getVehicle().getModel()) + "," +
                                    escapeCsv(booking.getVehicle().getCategory()) + "," +
                                    booking.getVehicle().getPricePerDay() + "," +
                                    booking.getStartDate() + "," +
                                    booking.getEndDate() + "," +
                                    escapeCsv(booking.getStatus())
                    );
                }

                showAlert("Success", "Bookings data exported successfully to " + file.getName());
            } catch (FileNotFoundException e) {
                showAlert("Error", "Failed to export bookings data: " + e.getMessage());
            }
        }
    }

    private void showCreateBookingDialog() {
        Dialog<Booking> dialog = new Dialog<>();
        dialog.setTitle("Create New Booking");
        dialog.setHeaderText("Select customer and available vehicle for booking");

        // Set up form
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(20, 20, 20, 20));

        // Customer selection
        Label customerLabel = new Label("Customer:");
        ComboBox<Customer> customerCombo = new ComboBox<>();
        customerCombo.setItems(customers);
        customerCombo.setPromptText("Select Customer");
        customerCombo.setCellFactory(lv -> new ListCell<Customer>() {
            @Override
            protected void updateItem(Customer customer, boolean empty) {
                super.updateItem(customer, empty);
                setText(empty ? null : customer.getName() + " (" + customer.getPhone() + ")");
            }
        });
        customerCombo.setButtonCell(new ListCell<Customer>() {
            @Override
            protected void updateItem(Customer customer, boolean empty) {
                super.updateItem(customer, empty);
                setText(empty ? null : customer == null ? "Select Customer" :
                        customer.getName() + " (" + customer.getPhone() + ")");
            }
        });

        // Vehicle selection (only available vehicles)
        Label vehicleLabel = new Label("Available Vehicles:");
        ComboBox<Vehicle> vehicleCombo = new ComboBox<>();
        vehicleCombo.setItems(FXCollections.observableArrayList(
                vehicles.filtered(Vehicle::isAvailable)
        ));
        vehicleCombo.setPromptText("Select Vehicle");
        vehicleCombo.setCellFactory(lv -> new ListCell<Vehicle>() {
            @Override
            protected void updateItem(Vehicle vehicle, boolean empty) {
                super.updateItem(vehicle, empty);
                setText(empty ? null : vehicle.getBrand() + " " + vehicle.getModel() +
                        " (" + vehicle.getCategory() + ") - $" + vehicle.getPricePerDay() + "/day");
            }
        });
        vehicleCombo.setButtonCell(new ListCell<Vehicle>() {
            @Override
            protected void updateItem(Vehicle vehicle, boolean empty) {
                super.updateItem(vehicle, empty);
                setText(empty ? null : vehicle == null ? "Select Vehicle" :
                        vehicle.getBrand() + " " + vehicle.getModel());
            }
        });

        // Date pickers
        Label dateLabel = new Label("Booking Dates:");
        DatePicker startDatePicker = new DatePicker();
        startDatePicker.setPromptText("Start Date");
        startDatePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now()));
            }
        });
        DatePicker endDatePicker = new DatePicker();
        endDatePicker.setPromptText("End Date");
        endDatePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now().plusDays(1)));
            }
        });

        // Price calculation label
        Label priceLabel = new Label("Total Price: $0.00");
        priceLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        // Layout
        grid.add(customerLabel, 0, 0);
        grid.add(customerCombo, 1, 0);
        grid.add(vehicleLabel, 0, 1);
        grid.add(vehicleCombo, 1, 1);
        grid.add(dateLabel, 0, 2);

        HBox dateBox = new HBox(10);
        dateBox.getChildren().addAll(startDatePicker, new Label("to"), endDatePicker);
        grid.add(dateBox, 1, 2);

        grid.add(priceLabel, 0, 3, 2, 1);

        // Add listeners for price calculation
        vehicleCombo.valueProperty().addListener((obs, oldVal, newVal) ->
                updatePriceLabel(priceLabel, vehicleCombo, startDatePicker, endDatePicker));
        startDatePicker.valueProperty().addListener((obs, oldVal, newVal) ->
                updatePriceLabel(priceLabel, vehicleCombo, startDatePicker, endDatePicker));
        endDatePicker.valueProperty().addListener((obs, oldVal, newVal) ->
                updatePriceLabel(priceLabel, vehicleCombo, startDatePicker, endDatePicker));

        dialog.getDialogPane().setContent(grid);

        // Add buttons
        ButtonType createButton = new ButtonType("Create Booking", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButton, ButtonType.CANCEL);

        // Convert result to Booking when Create button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createButton) {
                if (customerCombo.getValue() == null) {
                    showAlert("Missing Information", "Please select a customer");
                    return null;
                }
                if (vehicleCombo.getValue() == null) {
                    showAlert("Missing Information", "Please select a vehicle");
                    return null;
                }
                if (startDatePicker.getValue() == null || endDatePicker.getValue() == null) {
                    showAlert("Missing Information", "Please select both start and end dates");
                    return null;
                }
                if (startDatePicker.getValue().isAfter(endDatePicker.getValue())) {
                    showAlert("Invalid Dates", "End date must be after start date");
                    return null;
                }

                return new Booking(0, customerCombo.getValue(), vehicleCombo.getValue(),
                        startDatePicker.getValue(), endDatePicker.getValue(), "Active");
            }
            return null;
        });

        Optional<Booking> result = dialog.showAndWait();
        result.ifPresent(booking -> {
            try {
                if (addBookingToDatabase(booking)) {
                    // Mark vehicle as unavailable
                    booking.getVehicle().setAvailable(false);
                    updateVehicleInDatabase(booking.getVehicle());

                    bookings.add(booking);

                    // Show booking confirmation
                    long days = booking.getEndDate().toEpochDay() - booking.getStartDate().toEpochDay() + 1;
                    double totalPrice = days * booking.getVehicle().getPricePerDay();

                    Alert confirmation = new Alert(Alert.AlertType.INFORMATION);
                    confirmation.setTitle("Booking Confirmation");
                    confirmation.setHeaderText("Booking Created Successfully");
                    confirmation.setContentText(
                            "Customer: " + booking.getCustomer().getName() + "\n" +
                                    "Vehicle: " + booking.getVehicle().getBrand() + " " + booking.getVehicle().getModel() + "\n" +
                                    "Category: " + booking.getVehicle().getCategory() + "\n" +
                                    "Price/Day: $" + booking.getVehicle().getPricePerDay() + "\n" +
                                    "Dates: " + booking.getStartDate() + " to " + booking.getEndDate() + "\n" +
                                    "Total Days: " + days + "\n" +
                                    "Total Price: $" + String.format("%.2f", totalPrice)
                    );
                    confirmation.showAndWait();
                } else {
                    showAlert("Error", "Failed to create booking");
                }
            } catch (SQLException ex) {
                showAlert("Database Error", "Failed to create booking: " + ex.getMessage());
            }
        });
    }

    private void updatePriceLabel(Label priceLabel, ComboBox<Vehicle> vehicleCombo,
                                  DatePicker startDatePicker, DatePicker endDatePicker) {
        if (vehicleCombo.getValue() != null && startDatePicker.getValue() != null &&
                endDatePicker.getValue() != null && !startDatePicker.getValue().isAfter(endDatePicker.getValue())) {

            long days = endDatePicker.getValue().toEpochDay() - startDatePicker.getValue().toEpochDay() + 1;
            double totalPrice = days * vehicleCombo.getValue().getPricePerDay();
            priceLabel.setText(String.format("Total Price: $%.2f (for %d days)", totalPrice, days));
        } else {
            priceLabel.setText("Total Price: $0.00");
        }
    }

    private boolean addBookingToDatabase(Booking booking) throws SQLException {
        String query = "INSERT INTO bookings (customer_id, vehicle_id, start_date, end_date, status) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, booking.getCustomer().getId());
            stmt.setInt(2, booking.getVehicle().getId());
            stmt.setDate(3, java.sql.Date.valueOf(booking.getStartDate()));
            stmt.setDate(4, java.sql.Date.valueOf(booking.getEndDate()));
            stmt.setString(5, booking.getStatus());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        booking.setId(rs.getInt(1));
                        return true;
                    }
                }
            }
            return false;
        }
    }

    private void cancelBooking(Booking booking) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Cancellation");
        alert.setHeaderText("Cancel Booking");
        alert.setContentText("Are you sure you want to cancel this booking?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                String query = "UPDATE bookings SET status = 'Cancelled' WHERE booking_id = ?";
                try (PreparedStatement stmt = connection.prepareStatement(query)) {
                    stmt.setInt(1, booking.getId());

                    if (stmt.executeUpdate() > 0) {
                        booking.setStatus("Cancelled");
                        // Mark vehicle as available again
                        booking.getVehicle().setAvailable(true);
                        updateVehicleInDatabase(booking.getVehicle());

                        showAlert("Success", "Booking cancelled successfully");
                    } else {
                        showAlert("Error", "Failed to cancel booking");
                    }
                }
            } catch (SQLException e) {
                showAlert("Database Error", "Failed to cancel booking: " + e.getMessage());
            }
        }
    }

    private void completeBooking(Booking booking) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Complete Booking");
        alert.setHeaderText("Complete Booking");
        alert.setContentText("Are you sure you want to mark this booking as completed?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                String query = "UPDATE bookings SET status = 'Completed' WHERE booking_id = ?";
                try (PreparedStatement stmt = connection.prepareStatement(query)) {
                    stmt.setInt(1, booking.getId());

                    if (stmt.executeUpdate() > 0) {
                        booking.setStatus("Completed");
                        // Mark vehicle as available again
                        booking.getVehicle().setAvailable(true);
                        updateVehicleInDatabase(booking.getVehicle());

                        showAlert("Success", "Booking completed successfully");

                        // Show payment dialog
                        showPaymentDialog(booking);
                    } else {
                        showAlert("Error", "Failed to complete booking");
                    }
                }
            } catch (SQLException e) {
                showAlert("Database Error", "Failed to complete booking: " + e.getMessage());
            }
        }
    }

    private void showPaymentDialog(Booking booking) {
        Dialog<Payment> dialog = new Dialog<>();
        dialog.setTitle("Process Payment");

        // Calculate total amount
        long days = booking.getEndDate().toEpochDay() - booking.getStartDate().toEpochDay() + 1;
        double amount = days * booking.getVehicle().getPricePerDay();

        // Set up form
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        Label bookingLabel = new Label("Booking Details:");
        Label customerLabel = new Label("Customer: " + booking.getCustomer().getName());
        Label vehicleLabel = new Label("Vehicle: " + booking.getVehicle().getBrand() + " " + booking.getVehicle().getModel());
        Label datesLabel = new Label("Dates: " + booking.getStartDate() + " to " + booking.getEndDate());
        Label daysLabel = new Label("Days: " + days);
        Label amountLabel = new Label(String.format("Amount: $%.2f", amount));

        ComboBox<String> methodCombo = new ComboBox<>();
        methodCombo.getItems().addAll("Cash", "Credit Card", "Debit Card", "Bank Transfer");
        methodCombo.setValue("Credit Card");

        grid.add(bookingLabel, 0, 0, 2, 1);
        grid.add(customerLabel, 0, 1, 2, 1);
        grid.add(vehicleLabel, 0, 2, 2, 1);
        grid.add(datesLabel, 0, 3, 2, 1);
        grid.add(daysLabel, 0, 4, 2, 1);
        grid.add(amountLabel, 0, 5, 2, 1);
        grid.add(new Label("Payment Method:"), 0, 6);
        grid.add(methodCombo, 1, 6);

        dialog.getDialogPane().setContent(grid);

        // Add buttons
        ButtonType processButton = new ButtonType("Process Payment", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(processButton, ButtonType.CANCEL);

        // Convert result to Payment when Process button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == processButton) {
                return new Payment(0, booking, amount, methodCombo.getValue(), "Completed", LocalDate.now());
            }
            return null;
        });

        Optional<Payment> result = dialog.showAndWait();
        result.ifPresent(payment -> {
            try {
                if (addPaymentToDatabase(payment)) {
                    showAlert("Success", "Payment processed successfully");
                } else {
                    showAlert("Error", "Failed to process payment");
                }
            } catch (SQLException ex) {
                showAlert("Database Error", "Failed to process payment: " + ex.getMessage());
            }
        });
    }

    private boolean addPaymentToDatabase(Payment payment) throws SQLException {
        String query = "INSERT INTO payments (booking_id, amount, method, status, payment_date) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, payment.getBooking().getId());
            stmt.setDouble(2, payment.getAmount());
            stmt.setString(3, payment.getMethod());
            stmt.setString(4, payment.getStatus());
            stmt.setDate(5, java.sql.Date.valueOf(payment.getPaymentDate()));

            return stmt.executeUpdate() > 0;
        }
    }

    private Node createPaymentBillingTab() {
        VBox paymentBilling = new VBox(20);
        paymentBilling.setPadding(new Insets(20));

        // Add export button
        Button exportPaymentsButton = new Button("Export to CSV");
        exportPaymentsButton.setOnAction(e -> exportPaymentsToCSV());

        // Payment table
        TableView<Payment> paymentTable = new TableView<>();

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT p.payment_id, p.amount, p.method, p.status, p.payment_date, " +
                     "b.booking_id, b.start_date, b.end_date, " +
                     "v.vehicle_id, v.brand, v.model, " +
                     "c.customer_id, c.name " +
                     "FROM payments p " +
                     "JOIN bookings b ON p.booking_id = b.booking_id " +
                     "JOIN vehicles v ON b.vehicle_id = v.vehicle_id " +
                     "JOIN customers c ON b.customer_id = c.customer_id")) {

            ObservableList<Payment> payments = FXCollections.observableArrayList();
            while (rs.next()) {
                Vehicle vehicle = new Vehicle(
                        rs.getInt("vehicle_id"),
                        rs.getString("brand"),
                        rs.getString("model"),
                        "", 0, false);

                Customer customer = new Customer(
                        rs.getInt("customer_id"),
                        rs.getString("name"),
                        "", "", "");

                Booking booking = new Booking(
                        rs.getInt("booking_id"),
                        customer,
                        vehicle,
                        rs.getDate("start_date").toLocalDate(),
                        rs.getDate("end_date").toLocalDate(),
                        "");

                payments.add(new Payment(
                        rs.getInt("payment_id"),
                        booking,
                        rs.getDouble("amount"),
                        rs.getString("method"),
                        rs.getString("status"),
                        rs.getDate("payment_date").toLocalDate())
                );
            }

            paymentTable.setItems(payments);
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to load payments: " + e.getMessage());
        }

        TableColumn<Payment, String> customerCol = new TableColumn<>("Customer");
        customerCol.setCellValueFactory(cell -> cell.getValue().getBooking().getCustomer().nameProperty());

        TableColumn<Payment, String> vehicleCol = new TableColumn<>("Vehicle");
        vehicleCol.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getBooking().getVehicle().getBrand() + " " +
                        cell.getValue().getBooking().getVehicle().getModel()));

        TableColumn<Payment, Double> amountCol = new TableColumn<>("Amount");
        amountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        amountCol.setCellFactory(col -> new TableCell<Payment, Double>() {
            @Override
            protected void updateItem(Double amount, boolean empty) {
                super.updateItem(amount, empty);
                if (empty || amount == null) {
                    setText(null);
                } else {
                    setText(String.format("$%.2f", amount));
                }
            }
        });

        TableColumn<Payment, String> methodCol = new TableColumn<>("Method");
        methodCol.setCellValueFactory(new PropertyValueFactory<>("method"));

        TableColumn<Payment, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setCellFactory(col -> new TableCell<Payment, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                } else {
                    setText(status);
                    if (status.equals("Completed")) {
                        setTextFill(Color.GREEN);
                    } else if (status.equals("Pending")) {
                        setTextFill(Color.ORANGE);
                    } else if (status.equals("Failed")) {
                        setTextFill(Color.RED);
                    }
                }
            }
        });

        TableColumn<Payment, LocalDate> dateCol = new TableColumn<>("Payment Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("paymentDate"));

        paymentTable.getColumns().addAll(customerCol, vehicleCol, amountCol, methodCol, statusCol, dateCol);
        paymentTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        paymentBilling.getChildren().addAll(exportPaymentsButton, paymentTable);
        return paymentBilling;
    }

    private void exportPaymentsToCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Payments Data");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName("payments_data.csv");

        File file = fileChooser.showSaveDialog(root.getScene().getWindow());
        if (file != null) {
            try (PrintWriter writer = new PrintWriter(file)) {
                // Write CSV header
                writer.println("ID,Customer,Vehicle,Amount,Method,Status,Payment Date");

                // Write data
                try (Statement stmt = connection.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT p.payment_id, p.amount, p.method, p.status, p.payment_date, " +
                             "c.name AS customer_name, v.brand, v.model " +
                             "FROM payments p " +
                             "JOIN bookings b ON p.booking_id = b.booking_id " +
                             "JOIN vehicles v ON b.vehicle_id = v.vehicle_id " +
                             "JOIN customers c ON b.customer_id = c.customer_id")) {

                    while (rs.next()) {
                        writer.println(
                                rs.getInt("payment_id") + "," +
                                        escapeCsv(rs.getString("customer_name")) + "," +
                                        escapeCsv(rs.getString("brand") + " " + rs.getString("model")) + "," +
                                        rs.getDouble("amount") + "," +
                                        escapeCsv(rs.getString("method")) + "," +
                                        escapeCsv(rs.getString("status")) + "," +
                                        rs.getDate("payment_date")
                        );
                    }
                }

                showAlert("Success", "Payments data exported successfully to " + file.getName());
            } catch (FileNotFoundException | SQLException e) {
                showAlert("Error", "Failed to export payments data: " + e.getMessage());
            }
        }
    }

    private Node createReportsTab() {
        VBox reports = new VBox(20);
        reports.setPadding(new Insets(20));

        // Add export buttons
        HBox buttonBox = new HBox(10);
        Button exportRevenueBtn = new Button("Export Revenue Data");
        Button exportUtilizationBtn = new Button("Export Utilization Data");
        buttonBox.getChildren().addAll(exportRevenueBtn, exportUtilizationBtn);

        // Revenue chart
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> revenueChart = new BarChart<>(xAxis, yAxis);
        revenueChart.setTitle("Monthly Revenue");
        xAxis.setLabel("Month");
        yAxis.setLabel("Amount ($)");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Revenue");

        // Sample data - in a real app, you would query this from the database
        series.getData().add(new XYChart.Data<>("Jan", 12000));
        series.getData().add(new XYChart.Data<>("Feb", 15000));
        series.getData().add(new XYChart.Data<>("Mar", 18000));
        series.getData().add(new XYChart.Data<>("Apr", 9000));
        series.getData().add(new XYChart.Data<>("May", 11000));

        revenueChart.getData().add(series);

        // Vehicle utilization pie chart
        PieChart utilizationChart = new PieChart();
        utilizationChart.setTitle("Vehicle Utilization");

        // Sample data - in a real app, you would query this from the database
        utilizationChart.getData().add(new PieChart.Data("Available", 25));
        utilizationChart.getData().add(new PieChart.Data("Booked", 15));
        utilizationChart.getData().add(new PieChart.Data("Maintenance", 5));

        // Set up button actions
        exportRevenueBtn.setOnAction(e -> exportRevenueData(series));
        exportUtilizationBtn.setOnAction(e -> exportUtilizationData(utilizationChart));

        reports.getChildren().addAll(buttonBox, revenueChart, utilizationChart);
        return reports;
    }

    private void exportRevenueData(XYChart.Series<String, Number> series) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Revenue Report");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName("revenue_report.csv");

        File file = fileChooser.showSaveDialog(root.getScene().getWindow());
        if (file != null) {
            try (PrintWriter writer = new PrintWriter(file)) {
                // Write CSV header
                writer.println("Month,Revenue");

                // Write data
                for (XYChart.Data<String, Number> data : series.getData()) {
                    writer.println(data.getXValue() + "," + data.getYValue());
                }

                showAlert("Success", "Revenue data exported successfully to " + file.getName());
            } catch (FileNotFoundException e) {
                showAlert("Error", "Failed to export revenue data: " + e.getMessage());
            }
        }
    }

    private void exportUtilizationData(PieChart utilizationChart) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Utilization Report");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName("utilization_report.csv");

        File file = fileChooser.showSaveDialog(root.getScene().getWindow());
        if (file != null) {
            try (PrintWriter writer = new PrintWriter(file)) {
                // Write CSV header
                writer.println("Status,Count");

                // Write data
                for (PieChart.Data data : utilizationChart.getData()) {
                    writer.println(escapeCsv(data.getName()) + "," + data.getPieValue());
                }

                showAlert("Success", "Utilization data exported successfully to " + file.getName());
            } catch (FileNotFoundException e) {
                showAlert("Error", "Failed to export utilization data: " + e.getMessage());
            }
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Data model classes
    public static class User {
        private final int id;
        private final String username;
        private final String role;

        public User(int id, String username, String role) {
            this.id = id;
            this.username = username;
            this.role = role;
        }

        public int getId() { return id; }
        public String getUsername() { return username; }
        public String getRole() { return role; }
    }

    public static class Vehicle {
        private int id;
        private String brand;
        private String model;
        private String category;
        private double pricePerDay;
        private boolean available;

        public Vehicle(int id, String brand, String model, String category, double pricePerDay, boolean available) {
            this.id = id;
            this.brand = brand;
            this.model = model;
            this.category = category;
            this.pricePerDay = pricePerDay;
            this.available = available;
        }

        // Getters and setters
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getBrand() { return brand; }
        public void setBrand(String brand) { this.brand = brand; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public double getPricePerDay() { return pricePerDay; }
        public void setPricePerDay(double pricePerDay) { this.pricePerDay = pricePerDay; }
        public boolean isAvailable() { return available; }
        public void setAvailable(boolean available) { this.available = available; }
    }

    public static class Customer {
        private int id;
        private String name;
        private String phone;
        private String email;
        private String licenseNumber;

        public Customer(int id, String name, String phone, String email, String licenseNumber) {
            this.id = id;
            this.name = name;
            this.phone = phone;
            this.email = email;
            this.licenseNumber = licenseNumber;
        }

        // Getters and setters
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getLicenseNumber() { return licenseNumber; }
        public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }

        public StringProperty nameProperty() { return new SimpleStringProperty(name); }
        public StringProperty phoneProperty() { return new SimpleStringProperty(phone); }
        public StringProperty emailProperty() { return new SimpleStringProperty(email); }
        public StringProperty licenseNumberProperty() { return new SimpleStringProperty(licenseNumber); }
    }

    public static class Booking {
        private int id;
        private final Customer customer;
        private final Vehicle vehicle;
        private LocalDate startDate;
        private LocalDate endDate;
        private String status;

        public Booking(int id, Customer customer, Vehicle vehicle, LocalDate startDate, LocalDate endDate, String status) {
            this.id = id;
            this.customer = customer;
            this.vehicle = vehicle;
            this.startDate = startDate;
            this.endDate = endDate;
            this.status = status;
        }

        // Getters and setters
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public Customer getCustomer() { return customer; }
        public Vehicle getVehicle() { return vehicle; }
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public ObjectProperty<Customer> customerProperty() { return new SimpleObjectProperty<>(customer); }
    }

    public static class Payment {
        private final int id;
        private final Booking booking;
        private final double amount;
        private final String method;
        private final String status;
        private final LocalDate paymentDate;

        public Payment(int id, Booking booking, double amount, String method, String status, LocalDate paymentDate) {
            this.id = id;
            this.booking = booking;
            this.amount = amount;
            this.method = method;
            this.status = status;
            this.paymentDate = paymentDate;
        }

        // Getters
        public int getId() { return id; }
        public Booking getBooking() { return booking; }
        public double getAmount() { return amount; }
        public String getMethod() { return method; }
        public String getStatus() { return status; }
        public LocalDate getPaymentDate() { return paymentDate; }
    }
}