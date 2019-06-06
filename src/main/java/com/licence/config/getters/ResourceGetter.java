package com.licence.config.getters;

import org.apache.commons.io.FileUtils;

import java.io.File;

// get the content of a file with or without replacing
public class ResourceGetter {

    private String path;

    public ResourceGetter(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getFileToString(String whatToReplace, String withWhatToReplace) {
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            File file = new File(classLoader.getResource(path).getFile());
            String content = FileUtils.readFileToString(file, "UTF-8");
            if (whatToReplace != null)
                return content.replaceAll(whatToReplace, withWhatToReplace);
            else return content;
        } catch (Exception e) {
            System.out.println("StartUpCql could not been loaded!");
            return null;
        }
    }

}
