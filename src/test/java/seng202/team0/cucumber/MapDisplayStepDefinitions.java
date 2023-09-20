package seng202.team0.cucumber;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.jupiter.api.Assertions;
import seng202.team0.App;
import seng202.team0.models.JavaScriptBridge;

public class MapDisplayStepDefinitions {

    @When("the user is at the landing page")
    public void showingLandingPage() {
        // Do nothing
    }

    @Then("the map shows crashes from the database")
    public void showTableDataOnMap() {
        JavaScriptBridge bridge = new JavaScriptBridge();
        Assertions.assertTrue(bridge.crashes().length() > 0);
    }
}
