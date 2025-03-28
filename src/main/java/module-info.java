open module moss {

    requires static lombok;

    requires org.slf4j;
    requires dashscope4j;
    requires io.reactivex.rxjava3;
    requires okhttp3;

    requires spring.boot.autoconfigure;
    requires spring.context;
    requires spring.beans;
    requires spring.boot;
    requires spring.core;

    requires javafx.fxml;
    requires javafx.web;
    requires javafx.controls;

    requires org.commonmark;
    requires org.commonmark.ext.gfm.tables;

    requires org.mybatis;
    requires org.xerial.sqlitejdbc;
    requires annotations;
    requires jakarta.annotation;
    requires org.apache.commons.io;
    requires org.apache.commons.text;
    requires com.fasterxml.jackson.databind;

}