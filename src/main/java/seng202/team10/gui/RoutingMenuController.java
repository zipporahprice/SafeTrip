package seng202.team10.gui;

import java.net.URL;
import java.sql.SQLException;
import java.util.*;

import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.text.Font;
import javafx.util.Duration;
import javafx.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.control.PopOver;
import seng202.team10.business.FilterManager;
import seng202.team10.business.RouteManager;
import seng202.team10.business.SettingsManager;
import seng202.team10.models.*;
import seng202.team10.repository.SqliteQueryBuilder;

import static seng202.team10.business.RouteManager.getOverlappingPoints;

/**
 * The `RoutingMenuController` class manages user
 * interactions related to routing and displaying routes
 * in our application. It implements the `Initializable`
 * and `MenuController` interfaces for initialization
 * and updating of routing settings. This class allows
 * users to input start, end, and stop locations for route
 * generation, select a transport mode, and view route
 * information, including the number of crashes along the route
 * and a danger rating.
 *
 * @author Team 10
 */
public class RoutingMenuController implements Initializable, MenuController {

    private static final Logger log = LogManager.getLogger(RoutingMenuController.class);

    @FXML
    private ComboBox<String> startLocation;
    @FXML
    private ComboBox<String> endLocation;
    @FXML
    private ComboBox<String> stopLocation;
    @FXML
    private Label numCrashesLabel;
    @FXML
    private Button carButton;
    @FXML
    private Button bikeButton;
    @FXML
    private Button walkingButton;
    @FXML
    Button generateRoute;
    @FXML
    private Button removeRoute;
    @FXML
    ListView<String> stopsListView = new ListView<>();
    @FXML
    ListView<String> favouritesListView = new ListView<>();

    private static List<Location> matchedPoints;
    public static RoutingMenuController controller;
    private GeoLocator geolocator;
    private List<Location> stops = new ArrayList<>();
    private Button selectedButton = null;
    private String modeChoice;
    private String startAddress;
    private String endAddress;
    private String stopAddress;
    private PopOver popOver;
    private final List<Button> transportButtons = new ArrayList<>();
    private ObservableList<String> stopStrings = FXCollections.observableArrayList();
    private ObservableList<String> favouriteStrings = FXCollections.observableArrayList();


    /**
     * Initializes the JavaFX controller when the associated FXML file is loaded.
     * Creates an instance of the GeoLocater Class which is used to
     * find the locations and create the routes

     * @param url            The location of the FXML file.
     * @param resourceBundle The resource bundle.
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        geolocator = new GeoLocator();
        carButton.setUserData("car");
        bikeButton.setUserData("bike");
        walkingButton.setUserData("walking");
        transportButtons.add(carButton);
        transportButtons.add(bikeButton);
        transportButtons.add(walkingButton);
        removeRoute.setDisable(true);
        stopsListView.setItems(stopStrings);
        stopsListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        favouritesListView.setItems(favouriteStrings);
        favouritesListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        controller = this;
        loadManager();
    }


    /**
     * Displays a notification message near the specified button when it is pressed.
     *
     * @param btn     The button for which the notification is displayed.
     * @param message The message to be displayed in the notification.
     */
    private void showNotificationOnButtonPress(Button btn, String message) {
        if (popOver != null && popOver.isShowing()) {
            popOver.hide();
        }
        Label label = new Label(message);
        label.setFont(new Font(20.0));
        label.setPadding(new Insets(5));
        popOver = new PopOver(label);
        popOver.setArrowLocation(PopOver.ArrowLocation.LEFT_CENTER);
        popOver.show(walkingButton);

        FadeTransition fadeOut = new FadeTransition(Duration.seconds(1.5),
                popOver.getSkin().getNode());

        fadeOut.setDelay(Duration.millis(1500));
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(event -> popOver.hide());
        fadeOut.play();
    }

    /**
     * Displays a route or routes based on safety score, mode choice, and an array of routes.
     *
     * @param routes      An array of Route objects to display.
     */
    private void displayRoute(Route... routes) {
        List<Route> routesList = new ArrayList<>();
        Collections.addAll(routesList, routes);
        if (modeChoice == null) {
            showNotificationOnButtonPress(generateRoute, "Please select a transport option");
        } else {
            MainController.javaScriptConnector.call("displayRoute", Route
                    .routesToJsonArray(routesList), modeChoice);
        }
    }

    /**
     * Displays a popover near a TextField with a specified message and fade-out duration.
     *
     * @param message   The message to be displayed in the popover.
     * @param textField The TextField near which the popover should be displayed.
     * @param time      The duration (in seconds) for the fade-out animation.
     */
    private void showPopOver(String message, ComboBox<String> textField, double time) {
        Label label = new Label(message);
        popOver = new PopOver(label);
        popOver.setArrowLocation(PopOver.ArrowLocation.LEFT_CENTER);
        popOver.show(textField);

        FadeTransition fadeOut = new FadeTransition(Duration.seconds(time),
                popOver.getSkin().getNode());

        fadeOut.setDelay(Duration.millis(1500));
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(event -> popOver.hide());
        fadeOut.play();

    }


    /**
     * Retrieves the start location based on user input from a TextField.
     *
     * @return The start Location object, or null if the input is empty or invalid.
     */
    @FXML
    private Location getStart() {
        String address = startAddress;
        System.out.println(startAddress);
        if (address == null) {
            return null;
        }
        Pair<Location, String> startResult = geolocator.getLocation(address);

        Location startMarker = startResult.getKey();
        String errorMessageStart = startResult.getValue();
        if (errorMessageStart != null) {
            showPopOver(errorMessageStart, startLocation, 5);
            return null;
        }

        return startMarker;
    }

    @FXML
    private void setStart() {
        String selectedItem = startLocation.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            startAddress = selectedItem;
        }
    }

    @FXML
    private void loadStartOptions() {
        String address = startLocation.getEditor().getText().trim();
        ObservableList<String> addressOptions = geolocator.getAddressOptions(address);
        startLocation.setItems(addressOptions);
        startLocation.getEditor().setText(address);
    }

    /**
     * Retrieves the end location based on user input from a TextField.
     *
     * @return The end Location object, or null if the input is empty or invalid.
     */
    @FXML
    private Location getEnd() {
        String address = endAddress;
        System.out.println(endAddress);
        if (address == null) {
            return null;
        }
        Pair<Location, String> endResult = geolocator.getLocation(address);

        Location endMarker = endResult.getKey();
        String errorEndMessage = endResult.getValue();
        if (errorEndMessage != null) {
            showPopOver(errorEndMessage, endLocation, 5);
            return null;
        }
        return endMarker;

    }

    @FXML
    private void setEnd() {
        String selectedItem = endLocation.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            endAddress = selectedItem;
        }
    }

    @FXML
    private void loadEndOptions() {
        String address = endLocation.getEditor().getText().trim();
        ObservableList<String> addressOptions = geolocator.getAddressOptions(address);
        endLocation.setItems(addressOptions);
        endLocation.getEditor().setText(address);

    }

    @FXML
    private Location getStop() {
        String address = stopAddress;
        if (address == null) {
            return null;
        }
        Pair<Location, String> stopResult = geolocator.getLocation(address);

        Location stopMarker = stopResult.getKey();
        String errorStopMessage = stopResult.getValue();
        if (errorStopMessage != null) {
            showPopOver(errorStopMessage, stopLocation, 5);
            return null;
        }

        return stopMarker;
    }

    @FXML
    private void setStop() {
        String selectedItem = stopLocation.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            stopAddress = selectedItem;
        }
    }

    @FXML
    private void loadStopOptions() {
        String address = stopLocation.getEditor().getText().trim();
        ObservableList<String> addressOptions = geolocator.getAddressOptions(address);
        stopLocation.setItems(addressOptions);
        stopLocation.getEditor().setText(address);

    }

    /**
     * Saves a route or favorite location to a database.
     *
     * @throws SQLException If a database error occurs during the save operation.
     */
    @FXML
    private void saveRoute() {
        Location start = getStart();
        Location end = getEnd();
        String filters = FilterManager.getInstance().toString();
        String startAddress = geolocator.getAddress(start.getLatitude(),
                start.getLongitude(), "Start");
        String endAddress = geolocator.getAddress(end.getLatitude(), end.getLongitude(), "End");
        String routeName = showRouteNameInputDialog();

        // List of favourite names
        List<String> favouriteNames = RouteManager.getFavouriteNames().stream().map((favourite) -> {
            HashMap<String, Object> favouriteHashmap = (HashMap<String, Object>) favourite;
            return (String) favouriteHashmap.get("route_name");
        }).toList();

        // Checks null, empty, and it is unique
        if (routeName == null || routeName.trim().isEmpty() || favouriteNames.contains(routeName)) {
            // Show error dialog
            showErrorDialog();
            return;
        }

        Favourite favourite = new Favourite(startAddress, endAddress,
                start.getLatitude(), start.getLongitude(), end.getLatitude(),
                end.getLongitude(), filters, modeChoice, routeName);

        List<Favourite> favourites = new ArrayList<>();
        favourites.add(favourite);

        SqliteQueryBuilder.create().insert("favourites").buildSetter(favourites);

        favouriteStrings.add(favourite.getName());
        favouritesListView.setItems(favouriteStrings);
    }

    private String showRouteNameInputDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Save Route");
        dialog.setHeaderText("Enter a name for the route:");
        Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
    }

    private void showErrorDialog() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Invalid name");
        alert.setContentText("Please provide a valid name for the route.");
        alert.showAndWait();
    }

    /**
     * Loads a selected route or favorite location from the ComboBox.
     *
     * @throws SQLException If a database error occurs during the loading operation.
     */
    @FXML
    private void loadRoute() throws SQLException {
        String routeName = favouritesListView.getSelectionModel().getSelectedItem();
        if (routeName != null) {
            List<?> favouriteList = SqliteQueryBuilder.create()
                    .select("*")
                    .from("favourites")
                    .where("route_name = \"" + routeName + "\"")
                    .buildGetter();
            System.out.println("hello");
            System.out.println(favouriteList.size());
            Favourite favourite = (Favourite) favouriteList.get(0);

            // Update FilterManager class with the filters associated with the favourite route
            FilterManager filters = FilterManager.getInstance();
            filters.updateFiltersWithQueryString(favourite.getFilters());

            // Generates a route and makes sure stops are cleared

            startLocation.getEditor().setText(favourite.getStartAddress());
            endLocation.getEditor().setText(favourite.getEndAddress());
            startAddress = favourite.getStartAddress();
            endAddress = favourite.getEndAddress();
            for (Button button : transportButtons) {
                if (button.getUserData().equals(favourite.getTransportMode())) {
                    selectButton(button);
                }
            }
            stops.clear();
            stopsListView.getItems().clear();
            generateRouteAction(favourite);
            favouritesListView.getSelectionModel().clearSelection();

        }
    }

    @FXML
    private void deleteRoute() {
        if (favouritesListView.getSelectionModel().getSelectedItem() != null) {
            int selectedStopIndex = favouritesListView.getSelectionModel().getSelectedIndex();
            String name = favouritesListView.getSelectionModel().getSelectedItem();
            SqliteQueryBuilder.create().delete("favourites").where("route_name = \"" + name + "\"").buildDeleter();
            favouriteStrings.remove(selectedStopIndex);
        } else {
            favouriteStrings.remove(stopStrings.size() - 1);
        }

        favouritesListView.setItems(favouriteStrings);
    }


    /**
     * Adds a stop location to the collection and generates a route action.
     *
     */
    @FXML
    private void addStop() {
        Location stop = getStop();
        if (stop != null) {
            stops.add(stop);
            stopStrings.add(stopLocation.getValue());
            stopLocation.getEditor().setText(null);
        }
        generateRouteAction();
    }

    /**
     * Removes the last stop from the collection and generates a route.
     *
     */
    @FXML
    private void removeStop() {
        if (stops.size() >= 1) {
            if (stopsListView.getSelectionModel().getSelectedItem() != null) {
                int selectedStopIndex = stopsListView.getSelectionModel().getSelectedIndex();
                stops.remove(selectedStopIndex);
                stopStrings.remove(selectedStopIndex);
            } else {
                stops.remove(stopStrings.size() - 1);
                stopStrings.remove(stopStrings.size() - 1);
            }

            generateRouteAction();
        }
    }

    public static void updateCrashes(List<?> crashes) {
        JavaScriptBridge.updateCrashesByJavascript(crashes);
        if (SettingsManager.getInstance().getCurrentView().equals("None")) {
            SettingsManager.getInstance().setCurrentView("Crash Locations");
        }
        MainController.javaScriptConnector.call("updateView");
    }

    /**
     * Generates a route, calculates its danger rating, and updates the UI.
     *
     */
    @FXML
    private void generateRouteAction() {
        Location start = getStart();
        Location end = getEnd();
        if (start != null && end != null) {
            routeLocations(start, end);
            removeRoute.setDisable(false);
        }
    }

    /**
     * Overloaded function for handling route generation with favourites.
     *
     * @param favourite Favourite object with locations of route and filters.
     */
    private void generateRouteAction(Favourite favourite) {
        Location start = new Location(favourite.getStartLat(), favourite.getStartLong());
        Location end = new Location(favourite.getEndLat(), favourite.getEndLong());

        routeLocations(start, end);
    }

    private void routeLocations(Location start, Location end) {
        List<Location> routeLocations = new ArrayList<>();
        routeLocations.add(start);
        routeLocations.addAll(stops);
        routeLocations.add(end);

        Route route = new Route(List.of(routeLocations.toArray(new Location[0])));
        displayRoute(route);
    }

    /**
     * Takes the list of coordinates stored in JavaScriptBridge and updates the rating shown
     * on the GUI's ratingText label through getting the overlapping points of each segment.
     */
    public static void ratingUpdate() {
        try {
            List<Location> coordinates =
                    JavaScriptBridge.getRouteMap().get(JavaScriptBridge.getIndex());
            List<String> roads = JavaScriptBridge.getRoadsMap().get(JavaScriptBridge.getIndex());
            List<Double> distances =
                    JavaScriptBridge.getDistancesMap().get(JavaScriptBridge.getIndex());
            if (coordinates != null && !coordinates.isEmpty()) {
                Review review = getOverlappingPoints(coordinates, roads, distances);
                updateCrashes(review.crashes);
                MainController.javaScriptConnector.call("updateReviewContent", review.toString());

            } else {
                System.out.println("No coordinates available for routeId: 0");
            }
        } catch (SQLException e) {
            log.error(e);
        }
    }

    /**
     * Calls the JS function, removeRoute.
     * When the corresponding button is pressed in the
     * GUI, this method is called and
     * the route is removed
     */
    @FXML
    private void removeRoute() {
        MainController.javaScriptConnector.call("removeRoute");
        removeRoute.setDisable(true);

    }


    /**
     * Enacts the selection of a given button when a click event occurs.
     * If the button is not already selected, it selects it.
     * Otherwise, it does nothing.
     *
     * @param event An ActionEvent called when the button is pressed.
     */
    public void toggleModeButton(ActionEvent event) {
        Button chosenButton = (Button) event.getSource();
        selectButton(chosenButton);
    }

    /**
     * Takes a button to be selected.
     * If a different button is already selected, deselects this button and selects the new one.
     * Otherwise, just selects the new one.
     *
     * @param chosenButton Button to be selected.
     */
    public void selectButton(Button chosenButton) {
        if (!Objects.equals(chosenButton, selectedButton)) {
            if (!Objects.equals(chosenButton, selectedButton) && selectedButton != null) {
                selectedButton.getStyleClass().remove("clickedButtonColor");
                selectedButton.getStyleClass().add("hamburgerStyle");
            }
            selectedButton = chosenButton;
            selectedButton.getStyleClass().remove("hamburgerStyle");
            selectedButton.getStyleClass().add("clickedButtonColor");
            modeChoice = (String) chosenButton.getUserData();
        }
    }

    /**
     * Loads the route data stored from the RouteManager into the routing menu.
     */
    @Override
    public void loadManager() {
        //List<String>
        favouriteStrings = FXCollections.observableArrayList(RouteManager.getFavouriteNames().stream().map((favourite) -> {
            HashMap<String, Object> favouriteHashmap = (HashMap<String, Object>) favourite;
            return (String) favouriteHashmap.get("route_name");
        }).toList());

        favouritesListView.getItems().addAll(favouriteStrings);

        RouteManager route = RouteManager.getInstance();

        // retrieve all updated location data
        String startLoc = route.getStartLocation();
        String endLoc = route.getEndLocation();
        String stopLoc = route.getStopLocation();
        String mode = route.getTransportMode();

        // update textFields according to data
        startLocation.getEditor().setText(startLoc);
        endLocation.getEditor().setText(endLoc);
        stopLocation.getEditor().setText(stopLoc);
        for (Button button : transportButtons) {
            if (button.getUserData().equals(mode)) {
                selectButton(button);
            }
        }
    }

    /**
     * Updates the RouteManager's stored data with data currently in the routing menu.
     */
    @Override
    public void updateManager() {
        RouteManager route = RouteManager.getInstance();
        route.setStartLocation(startLocation.getEditor().getText());
        route.setEndLocation(endLocation.getEditor().getText());
        route.setStopLocation(stopLocation.getEditor().getText());
        route.setTransportMode(modeChoice);
    }

}
