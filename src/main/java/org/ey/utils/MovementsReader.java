package org.ey.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class MovementsReader {
    static List<Map<String, String>> readMovements(String path){
        try(BufferedReader br = new BufferedReader(new FileReader(path))) {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();
            String[] headers = line.split("\\|");

            line= br.readLine();
            List<Map<String, String>> result = new ArrayList();
            while (line != null) {
                var data = line.split("\\|");
                var map = new HashMap<String, String>();
                for(int i=0; i< headers.length; i++){
                    map.put(headers[i], data[i]);
                }

                result.add(map);
                line = br.readLine();
            }
            return result;
        }
        catch (IOException e){
            e.printStackTrace();
            throw new RuntimeException("Error leyendo movimientos");
        }
    }

    static List<Map<String, String>> readMovements(){
        return readMovements("src/main/resources/portfolio-movements.txt");
    }
}
