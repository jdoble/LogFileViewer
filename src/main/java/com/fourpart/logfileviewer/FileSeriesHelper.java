package com.fourpart.logfileviewer;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Pattern;

public class FileSeriesHelper {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("[0-9]+");

    public static File[] getFileSeries(File[] files) {

        if (files.length > 1) {
            return sortFiles(files);
        }

        List<File> fileList = new LinkedList<>();

        File rootFile = files[0];

        fileList.add(rootFile);

        int index = 1;

        while (true) {

            File file = new File(rootFile.getAbsolutePath() + "." + index);

            if (file.exists()) {
                fileList.add(0, file);
                index++;
            }
            else {
                break;
            }
        }

        return fileList.toArray(new File[fileList.size()]);
    }

    private static File[] sortFiles(File[] files) {

        int shortestLength = Integer.MAX_VALUE;

        int shortestIndex = -1;

        for (int i = 0; i < files.length; i++) {

            String name = files[i].getName();

            if (name.length() < shortestLength) {
                shortestLength = name.length();
                shortestIndex = i;
            }
        }

        File rootFile = files[shortestIndex];

        String prefix = rootFile.getName() + ".";

        final int prefixLength = prefix.length();

        for (int i = 0; i < files.length; i++) {

            if (i == shortestIndex) {
                continue;
            }

            String fileName = files[i].getName();

            if (!fileName.startsWith(prefix)) {
                return files;
            }

            String suffix = fileName.substring(prefix.length());

            if (!NUMBER_PATTERN.matcher(suffix).matches()) {
                return files;
            }
        }

        TreeSet<File> sortedFiles = new TreeSet<>(new Comparator<File>() {
            @Override
            public int compare(File file1, File file2) {

                String file1Name = file1.getName();

                int file1Index = file1Name.length() < prefixLength
                        ? 0
                        : Integer.parseInt(file1Name.substring(prefixLength));

                String file2Name = file2.getName();

                int file2Index = file2Name.length() < prefixLength
                        ? 0
                        : Integer.parseInt(file2Name.substring(prefixLength));

                if (file1Index > file2Index) {
                    return -1;
                }

                if (file1Index < file2Index) {
                    return 1;
                }

                return 0;
            }
        });

        sortedFiles.addAll(Arrays.asList(files));

        return sortedFiles.toArray(new File[sortedFiles.size()]);
    }

    private static String getDefaultFileSeriesName(File[] files) {

        File parentFile = files[0].getAbsoluteFile().getParentFile();

        for (File file : files) {

            if (!parentFile.equals(file.getAbsoluteFile().getParentFile())) {
                return files[0].getName() + "...";
            }
        }

        return parentFile.getAbsolutePath() + File.separator + files[0].getName() + "...";
    }

    public static String getFileSeriesName(File[] files) {

        if (files.length == 1) {
            return files[0].getAbsolutePath();
        }

        File rootFile = files[files.length - 1];

        String filePrefix = rootFile.getAbsolutePath() + ".";

        int filePrefixLength = filePrefix.length();

        for (int i = 0; i < files.length - 1; i++) {

            String fileName = files[i].getAbsolutePath();

            if (!fileName.startsWith(filePrefix)) {
                return getDefaultFileSeriesName(files);
            }

            String suffix = fileName.substring(filePrefixLength);

            if (!NUMBER_PATTERN.matcher(suffix).matches()) {
                return getDefaultFileSeriesName(files);
            }
        }

        return rootFile.getAbsolutePath() + "*";
    }

    public static String getFileNamesString(File[] files) {

        StringBuilder sb = new StringBuilder();

        for (File file : files) {

            if (sb.length() > 0) {
                sb.append(", ");
            }

            sb.append(file.getName());
        }

        return sb.toString();
    }
}
