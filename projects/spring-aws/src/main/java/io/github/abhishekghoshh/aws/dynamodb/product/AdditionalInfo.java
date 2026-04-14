package io.github.abhishekghoshh.aws.dynamodb.product;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@DynamoDbBean
public class AdditionalInfo {
    private String manufacturer;
    private String warranty;

    public AdditionalInfo() {
    }

    public AdditionalInfo(String manufacturer, String warranty) {
        this.manufacturer = manufacturer;
        this.warranty = warranty;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getWarranty() {
        return warranty;
    }

    public void setWarranty(String warranty) {
        this.warranty = warranty;
    }
}
