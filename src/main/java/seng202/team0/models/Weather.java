package seng202.team0.models;

/**
 * Weather enum represents different weather conditions associated with a crash object.
 * It provides a mapping between string representations of weather conditions and enum constants.
 *
 * @author Zipporah Price
 */
public enum Weather {

    FINE("Fine"), LIGHTRAIN("Light rain"), HEAVYRAIN("Heavy rain"), MISTORFOG("Mist or fog"), SNOW("Snow"), HAILORSLEET("Hail or sleet"), NULL("Null");

    private final String name;

    Weather (String name) {
        this.name = name;
    }

    /**
     * Converts a string representation of weather into the corresponding Weather enum constant.
     *
     * @param stringWeather The string representation of the weather.
     * @return The Weather enum constant representing the given weather string, or null if not found.
     */
    public static Weather stringToWeather(String stringWeather) {
        switch(stringWeather) {
            case "Fine": return Weather.FINE;
            case "Light rain": return Weather.LIGHTRAIN;
            case "Heavy rain": return Weather.HEAVYRAIN;
            case "Mist or fog": return Weather.MISTORFOG;
            case "Snow": return Weather.SNOW;
            case "Hail or sleet": return Weather.HAILORSLEET;
            case "Null": return Weather.NULL;
            default: return null;
        }
    }

    public String getName() {
        return name;
    }
}
