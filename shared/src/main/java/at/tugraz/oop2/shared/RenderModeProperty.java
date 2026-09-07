package at.tugraz.oop2.shared;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;

public class RenderModeProperty {


    private final static Property<RenderMode> selected_render_mode = new SimpleObjectProperty<>();


    public static Property<RenderMode> optionProperty() {
            return selected_render_mode;
        }

    public RenderMode getSelectedMode() {
        return optionProperty().getValue();
    }

    public void setSelectedMode(RenderMode selected_mode) {
            optionProperty().setValue(selected_mode);
        }
}

