package com.nitramite.adapters;

public class HardwareItem {

    // Variables
    private HardwareType hardwareType;
    private Integer hardwareId;
    private String itemLetter, itemName, itemDescription;

    // Ammunition specific
    private Double ammunitionSpeed = null;                  // m/s (meters per second)
    private Double ammunitionWeight = null;                 // g (grams)
    private Double ammunitionDragCoefficientXValue = null;  // Drag coefficient X
    private Double ammunitionDragCoefficientYValue = null;  // Drag coefficient Y
    private Double ammunitionSizeMillisX = null;            // Width in millimeters
    private Double ammunitionSizeMillisY = null;            // Height in millimeters
    private Double ammunitionSizeMillisZ = null;            // Length in millimeters


    // Constructor
    public HardwareItem(final HardwareType hardwareType_, final Integer hardwareId_, final String itemLetter_, final String itemName_, final String itemDescription_) {
        this.hardwareType = hardwareType_;
        this.hardwareId = hardwareId_;
        this.itemLetter = itemLetter_;
        this.itemName = itemName_;
        this.itemDescription = itemDescription_;
    }

    // ---------------------------------------------------------------------------------------------
    /* Typical */

    public HardwareType getHardwareType() {
        return hardwareType;
    }

    public Integer getHardwareId() {
        return hardwareId;
    }

    public String getHardwareIdToString() {
        return String.valueOf(hardwareId);
    }

    public String getItemLetter() {
        return itemLetter;
    }

    public void setItemLetter(String itemLetter) {
        this.itemLetter = itemLetter;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getItemDescription() {
        return itemDescription;
    }

    public void setItemDescription(String itemDescription) {
        this.itemDescription = itemDescription;
    }

    // ---------------------------------------------------------------------------------------------
    /* Ammunition */

    /**
     * Important information for calculations are:
     * Ammunition speed, weight
     */

    public void setAmmunitionSpeed(Double ammunitionSpeed) {
        this.ammunitionSpeed = ammunitionSpeed;
    }

    public Double getAmmunitionSpeed() {
        return ammunitionSpeed;
    }

    public void setAmmunitionWeight(Double ammunitionWeight) {
        this.ammunitionWeight = ammunitionWeight;
    }

    public Double getAmmunitionWeight() {
        return ammunitionWeight;
    }

    public void setAmmunitionSizeMillisX(Double ammunitionSizeMillisX) {
        this.ammunitionSizeMillisX = ammunitionSizeMillisX;
    }

    public Double getAmmunitionSizeMillisX() {
        return ammunitionSizeMillisX;
    }

    public void setAmmunitionSizeMillisY(Double ammunitionSizeMillisY) {
        this.ammunitionSizeMillisY = ammunitionSizeMillisY;
    }

    public Double getAmmunitionSizeMillisY() {
        return ammunitionSizeMillisY;
    }

    public void setAmmunitionSizeMillisZ(Double ammunitionSizeMillisZ) {
        this.ammunitionSizeMillisZ = ammunitionSizeMillisZ;
    }

    public Double getAmmunitionSizeMillisZ() {
        return ammunitionSizeMillisZ;
    }

    public void setAmmunitionDragCoefficientXValue(Double ammunitionDragCoefficientXValue) {
        this.ammunitionDragCoefficientXValue = ammunitionDragCoefficientXValue;
    }

    public Double getAmmunitionDragCoefficientXValue() {
        return ammunitionDragCoefficientXValue;
    }

    public void setAmmunitionDragCoefficientYValue(Double ammunitionDragCoefficientYValue) {
        this.ammunitionDragCoefficientYValue = ammunitionDragCoefficientYValue;
    }

    public Double getAmmunitionDragCoefficientYValue() {
        return ammunitionDragCoefficientYValue;
    }

    // ---------------------------------------------------------------------------------------------

} // End of class