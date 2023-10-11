package com.fszarwacki.allezon.common;


import org.springframework.data.aerospike.mapping.Document;
import org.springframework.data.aerospike.mapping.Field;
import org.springframework.data.annotation.Id;

import com.aerospike.client.query.IndexType;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import org.springframework.data.aerospike.annotation.Indexed;


@Getter
@Setter
@Document(collection = "user-tag-events")
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class UserTagEvent {
    @Id
    private String time;
    @Indexed(name = "cookie_idx", type=IndexType.STRING)
    private String cookie;

    private String country;
    private Device device;
    private Action action;
    private String origin;

    @JsonProperty("product_info")
    private Product productInfo;

    @Indexed(name = "time_idx", type=IndexType.NUMERIC)
    @Field("time_mili")
    @EqualsAndHashCode.Exclude
    private Long timeInMiliseconds;

    @Override
    public String toString() {
        return "UserTagEvent [time=" + time + ", cookie=" + cookie + ", country=" + country + ", device=" + device
                + ", action=" + action + ", origin=" + origin + ", productInfo=" + productInfo + ", timeInMiliseconds="
                + timeInMiliseconds + "]";
    } 
}


/*

cookie: String  
events: Map
{  
    (timestamp: Long) -> UserTagEvent
}  


 */