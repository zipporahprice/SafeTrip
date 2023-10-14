package seng202.team0.gui;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.PieChart;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.AnchorPane;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import seng202.team0.App;
import seng202.team0.business.GraphManager;
import seng202.team0.repository.SqliteQueryBuilder;


/**
 * This class manages actions and views related to graphical representations of data.
 * It includes methods for initializing the window and handling navigation back to the main window.
 */
public class GraphController implements Initializable, MenuController {

    private static final Logger log = LogManager.getLogger(App.class);
    @FXML
    private PieChart pieChartMade;
    ObservableList<PieChart.Data> pieChartSqlTestData;

    private static String columnOfInterest = "region";
    @FXML
    private ChoiceBox chartChoiceBox;
    private String currentChart = "Pie Graph";
    @FXML
    private ComboBox chartDataComboBox;
    private static String currentChartData = "Region";
    @FXML
    private AnchorPane graphsDataPane;
    @FXML
    private Label holidayInfoLabel;
    @FXML
    private Label vehiclesInfoLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setChartOptions();
        setPieChartDataOptions();
        pieChartSqlTestData = newPieChartData(columnOfInterest);
        setPieGraph(pieChartMade, pieChartSqlTestData);
    }

    /**
     * Update the data manager associated with the menu.
     */
    @Override
    public void updateManager() {
        GraphManager graphingManager = GraphManager.getInstance();
        graphingManager.setCurrentColumnData(currentChartData);
        graphingManager.setCurrentColOfInterest(columnOfInterest);

    }

    /**
     * Load initial data and settings into the menu manager.
     */
    @Override
    public void loadManager() {
        GraphManager graphingManager = GraphManager.getInstance();

        String currentColumnData = graphingManager.getCurrentColumnData();
        currentChartData = currentColumnData;

        String currentColOfInterest = graphingManager.getCurrentColOfInterest();
        columnOfInterest = currentColOfInterest;

    }

    private void setPieGraph(PieChart pieGraph, ObservableList<PieChart.Data> pieData) {
        if (pieGraph.getData().size() != 0) {
            pieGraph.getData().clear();
        }

        for (PieChart.Data data : pieData) {
            pieGraph.getData().add(data);
        }

        pieGraph.setTitle("Crashes in Aotearoa by " + currentChartData);

        pieGraph.setLegendVisible(false);
        pieGraph.setLabelsVisible(true);
        pieGraph.setLabelLineLength(15);
        pieGraph.setMinSize(300, 300);
        pieGraph.setStartAngle(87);
        holidayInfoLabel.setVisible(false);
        vehiclesInfoLabel.setVisible(false);

        if (currentChartData.equals("Weather")) {
            pieGraph.setLegendVisible(true);
        } else if (currentChartData.equals("Holiday")) {
            holidayInfoLabel.setVisible(true);
        } else if (currentChartData.equals("Vehicle Type")) {
            vehiclesInfoLabel.setVisible(true);
        }

        setTooltipInfo(pieGraph);

    }

    private void setTooltipInfo(PieChart pieGraph) {
        int totalValue = pieGraph.getData()
                .stream().mapToInt(data -> (int) data.getPieValue()).sum();

        pieGraph.getData().forEach(data -> {
            String percentage = String.format("%.2f%%", (data.getPieValue() / totalValue * 100));
            String count = String.valueOf((int) data.getPieValue());
            String slice = data.getName();
            Tooltip toolTipPercentRegion = new Tooltip(percentage + ", count: "
                    + count + ", \n" + slice);
            Tooltip.install(data.getNode(), toolTipPercentRegion);
        });
    }

    private PieChart.Data createVehiclePieData(String vehicle, String columnWanted) {
        List<HashMap<Object, Object>> vehicleList;

        columnOfInterest = columnWanted;

        vehicleList = SqliteQueryBuilder.create()
                .select(columnOfInterest + ", COUNT(*)")
                .from("crashes")
                .groupBy(columnOfInterest)
                .build();

        ArrayList<String> sliceNames = new ArrayList<>();
        ArrayList<Double> sliceCounts = new ArrayList<>();

        for (HashMap<Object, Object> hash : vehicleList) {
            Object column = hash.get(columnOfInterest);
            double count = ((Number) hash.get("COUNT(*)")).doubleValue();
            sliceNames.add(column.toString());
            sliceCounts.add(count);
        }

        for (int i = 0; i < sliceNames.size(); i++) {
            String sliceName = sliceNames.get(i);
            if (sliceName.equals("1")) {
                sliceName = vehicle;
                PieChart.Data dataToAdd = new PieChart.Data(sliceName, sliceCounts.get(i));
                return dataToAdd;
            }

        }

        log.error("Invalid vehicle type!");

        return null;
    }

    private ObservableList<PieChart.Data> newPieChartVehicleData() {
        ObservableList<PieChart.Data> result = FXCollections.observableArrayList();

        PieChart.Data bikeData =  createVehiclePieData("Bicycle", "bicycle_involved");
        PieChart.Data busData =  createVehiclePieData("Bus", "bus_involved");
        PieChart.Data carData =  createVehiclePieData("Car", "car_involved");
        PieChart.Data mopedData =  createVehiclePieData("Moped", "moped_involved");
        PieChart.Data motorcycleData =  createVehiclePieData("Motorcycle", "motorcycle_involved");
        PieChart.Data parkedData =  createVehiclePieData("Parked Vehicle",
                "parked_vehicle_involved");
        PieChart.Data pedestrianData =  createVehiclePieData("Pedestrian", "pedestrian_involved");
        PieChart.Data schoolBusData =  createVehiclePieData("School Bus", "school_bus_involved");
        PieChart.Data trainData =  createVehiclePieData("Train", "train_involved");
        PieChart.Data truckData =  createVehiclePieData("Truck", "truck_involved");

        result.add(bikeData);
        result.add(busData);
        result.add(carData);
        result.add(mopedData);
        result.add(motorcycleData);
        result.add(parkedData);
        result.add(pedestrianData);
        result.add(schoolBusData);
        result.add(trainData);
        result.add(truckData);

        return result;
    }

    private ObservableList<PieChart.Data> newPieChartData(String columnOfInterest) {
        ObservableList<PieChart.Data> result = FXCollections.observableArrayList();
        List<HashMap<Object, Object>> dbList;

        if (columnOfInterest.equals("truck_involved")) {
            //because truck is the last data item to be set and
            // what columnOfInterest will be at the end.
            result = newPieChartVehicleData();
            return result;
        }

        dbList = SqliteQueryBuilder.create()
                .select(columnOfInterest + ", COUNT(*)")
                .from("crashes")
                .groupBy(columnOfInterest)
                .build();

        ArrayList<String> sliceNames = new ArrayList<>();
        ArrayList<Double> sliceCounts = new ArrayList<>();

        for (HashMap<Object, Object> hash : dbList) {
            Object column = hash.get(columnOfInterest);
            double count = ((Number) hash.get("COUNT(*)")).doubleValue();

            if (columnOfInterest.equals("severity")) {
                switch ((int) column) {
                    case 1 -> column = "Non-injury";
                    case 2 -> column = "Minor";
                    case 4 -> column = "Serious";
                    case 8 -> column = "Fatal";
                    default -> log.error("Invalid severity type");
                }
            } else if (columnOfInterest.equals("holiday")) {
                switch ((int) column) {
                    case 0 -> column = "Not a holiday";
                    case 1 -> column = "Was a holiday";
                    default -> log.error("Invalid holiday type");
                }
            }

            sliceNames.add(column.toString());
            sliceCounts.add(count);

        }

        for (int i = 0; i < sliceNames.size(); i++) {
            String sliceName = sliceNames.get(i);
            if (sliceName.equals("Manawatū-Whanganui")) {
                sliceName = "Manawatu-Whanganui"; //todo figure out macron
            } else if (sliceName.equals("Null")) {
                sliceName = "Unknown";
            }

            result.add(new PieChart.Data(sliceName, sliceCounts.get(i)));
        }

        return result;
    }

    /**
     * method to set chart options for dropdown, can only select 1 graph.
     */
    public void setChartOptions() {
        chartChoiceBox.getItems().addAll("Pie Graph", "Line Graph");
        chartChoiceBox.setValue(currentChart);
        if (currentChart.equals("Pie Graph")) {
            graphsDataPane.setVisible(true);
            //todo set line graph pane visibility false
        } else {
            graphsDataPane.setVisible(false);
            //TODO set line graph data options
        }
        chartChoiceBox.getSelectionModel()
                .selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        currentChart = (String) newValue;
                    }
                    // Adjusted for the new option.
                    switch (currentChart) {
                        case "Pie Graph":
                            // Code to show the Pie Graph.
                            //TODO refactor newPieChartData into here.
                            graphsDataPane.setVisible(true);
                            //todo set line graph pane visibility false
                            break;
                        case "Line Graph":
                            // Code to show Line Graph.
                            graphsDataPane.setVisible(false);
                            //todo set line graph pane visibility true

                            break;
                        default:
                            // Other cases.
                            log.error("uh oh wrong choiceBox option");
                            break;
                    }
                });
    }

    /**
     * Dropdown choosing the data for pie graph.
     */
    public void setPieChartDataOptions() {
        chartDataComboBox.getItems().addAll(
                "Region", "Holiday", "Severity", "Vehicle Type", "Weather", "Year");
        chartDataComboBox.setValue(currentChartData);

        chartDataComboBox.getSelectionModel()
                .selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        currentChartData = (String) newValue;
                    }
                    // Adjusted for the new option using enhanced switch.
                    switch (currentChartData) {
                        case "Region" -> columnOfInterest = "region";
                        case "Holiday" -> columnOfInterest = "holiday";
                        case "Severity" -> columnOfInterest = "severity";
                        case "Vehicle Type" -> //truck to trigger if statement in newPieChartData.
                                columnOfInterest = "truck_involved";
                        case "Weather" -> columnOfInterest = "weather";
                        case "Year" -> columnOfInterest = "crash_year";
                        default -> // Other cases.
                                log.error("Invalid comboBox option: " + currentChartData);
                    }

                    ObservableList<PieChart.Data> newPieData = newPieChartData(columnOfInterest);
                    pieChartMade.getData().removeAll();
                    pieChartMade.setVisible(false);
                    setPieGraph(pieChartMade, newPieData);
                    pieChartMade.setVisible(true);
                });
    }

}
