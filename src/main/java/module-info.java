open module moss {

    requires static lombok;
    requires static jakarta.annotation;
    requires static jakarta.validation;

    requires org.slf4j;
    requires dashscope4j;
    requires io.reactivex.rxjava3;
    requires okhttp3;
    requires org.mybatis;
    requires org.xerial.sqlitejdbc;
    requires com.fasterxml.jackson.databind;

    requires spring.boot;
    requires spring.boot.autoconfigure;
    requires spring.context;
    requires spring.beans;
    requires spring.core;

    requires javafx.fxml;
    requires javafx.web;
    requires javafx.controls;
    requires jdk.jsobject;
    requires java.desktop;

    requires org.commonmark;
    requires org.commonmark.ext.gfm.tables;

    requires org.apache.commons.io;
    requires org.apache.commons.text;
    requires org.apache.commons.lang3;
    requires micrometer.commons;

}