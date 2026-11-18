package sma.restaurante;

import sma.restaurante.datasource.DatabaseConnection;

import java.sql.Connection;

public class App {

    public static void main(String[] args) {

        try (Connection connection =
                     DatabaseConnection.getConnection()) {

            System.out.println(
                    "Conexion exitosa con Supabase"
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}