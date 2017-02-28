package com.newcontext.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Danny Purcell
 */
public class Cat {

    private String name;
    private int age;
    private String color;
    private String favoriteSpot;

    @JsonCreator
    public Cat(@JsonProperty("name") String name,
               @JsonProperty("age") int age,
               @JsonProperty("color") String color,
               @JsonProperty("favoriteSpot") String favoriteSpot) {
        this.name = name;
        this.age = age;
        this.color = color;
        this.favoriteSpot = favoriteSpot;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    public String getColor() {
        return color;
    }

    public String getFavoriteSpot() {
        return favoriteSpot;
    }

    @Override
    public String toString() {
        return String.format("Cat{name='%s', age=%d, color='%s', favoriteSpot='%s'}", name, age, color, favoriteSpot);
    }
}
