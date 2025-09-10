package com.chatbot.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * 文件操作工具类
 * 提供文件和目录的常用操作方法，使用Java 21特性和NIO.2 API
 */
public class FileUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(FileUtil.class);
    
    /**
     * 检查文件是否存在
     */
    public static boolean exists(String filePath) {
        if (StringUtil.isEmpty(filePath)) {
            return false;
        }
        return Files.exists(Paths.get(filePath));
    }
    
    /**
     * 检查文件是否存在
     */
    public static boolean exists(Path path) {
        return path != null && Files.exists(path);
    }
    
    /**
     * 检查是否为文件
     */
    public static boolean isFile(String filePath) {
        if (StringUtil.isEmpty(filePath)) {
            return false;
        }
        return Files.isRegularFile(Paths.get(filePath));
    }
    
    /**
     * 检查是否为目录
     */
    public static boolean isDirectory(String dirPath) {
        if (StringUtil.isEmpty(dirPath)) {
            return false;
        }
        return Files.isDirectory(Paths.get(dirPath));
    }
    
    /**
     * 创建目录（包括父目录）
     */
    public static boolean createDirectories(String dirPath) {
        if (StringUtil.isEmpty(dirPath)) {
            return false;
        }
        
        try {
            Files.createDirectories(Paths.get(dirPath));
            return true;
        } catch (IOException e) {
            logger.error("创建目录失败: " + dirPath, e);
            return false;
        }
    }
    
    /**
     * 创建文件（包括父目录）
     */
    public static boolean createFile(String filePath) {
        if (StringUtil.isEmpty(filePath)) {
            return false;
        }
        
        try {
            Path path = Paths.get(filePath);
            Files.createDirectories(path.getParent());
            Files.createFile(path);
            return true;
        } catch (IOException e) {
            logger.error("创建文件失败: " + filePath, e);
            return false;
        }
    }
    
    /**
     * 删除文件或目录
     */
    public static boolean delete(String path) {
        if (StringUtil.isEmpty(path)) {
            return false;
        }
        
        try {
            Path filePath = Paths.get(path);
            if (Files.isDirectory(filePath)) {
                return deleteDirectory(filePath);
            } else {
                Files.deleteIfExists(filePath);
                return true;
            }
        } catch (IOException e) {
            logger.error("删除失败: " + path, e);
            return false;
        }
    }
    
    /**
     * 递归删除目录
     */
    public static boolean deleteDirectory(Path directory) {
        if (!Files.exists(directory)) {
            return true;
        }
        
        try {
            Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }
                
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
            return true;
        } catch (IOException e) {
            logger.error("删除目录失败: " + directory, e);
            return false;
        }
    }
    
    /**
     * 复制文件
     */
    public static boolean copyFile(String sourcePath, String targetPath) {
        if (StringUtil.isEmpty(sourcePath) || StringUtil.isEmpty(targetPath)) {
            return false;
        }
        
        try {
            Path source = Paths.get(sourcePath);
            Path target = Paths.get(targetPath);
            Files.createDirectories(target.getParent());
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            logger.error("复制文件失败: " + sourcePath + " -> " + targetPath, e);
            return false;
        }
    }
    
    /**
     * 移动文件
     */
    public static boolean moveFile(String sourcePath, String targetPath) {
        if (StringUtil.isEmpty(sourcePath) || StringUtil.isEmpty(targetPath)) {
            return false;
        }
        
        try {
            Path source = Paths.get(sourcePath);
            Path target = Paths.get(targetPath);
            Files.createDirectories(target.getParent());
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            logger.error("移动文件失败: " + sourcePath + " -> " + targetPath, e);
            return false;
        }
    }
    
    /**
     * 读取文件内容为字符串
     */
    public static String readString(String filePath) {
        if (StringUtil.isEmpty(filePath)) {
            return null;
        }
        
        try {
            return Files.readString(Paths.get(filePath), StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error("读取文件失败: " + filePath, e);
            return null;
        }
    }
    
    /**
     * 读取文件所有行
     */
    public static List<String> readLines(String filePath) {
        if (StringUtil.isEmpty(filePath)) {
            return new ArrayList<>();
        }
        
        try {
            return Files.readAllLines(Paths.get(filePath), StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error("读取文件行失败: " + filePath, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 写入字符串到文件
     */
    public static boolean writeString(String filePath, String content) {
        if (StringUtil.isEmpty(filePath)) {
            return false;
        }
        
        try {
            Path path = Paths.get(filePath);
            Files.createDirectories(path.getParent());
            Files.writeString(path, content == null ? "" : content, StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            logger.error("写入文件失败: " + filePath, e);
            return false;
        }
    }
    
    /**
     * 写入行到文件
     */
    public static boolean writeLines(String filePath, List<String> lines) {
        if (StringUtil.isEmpty(filePath)) {
            return false;
        }
        
        try {
            Path path = Paths.get(filePath);
            Files.createDirectories(path.getParent());
            Files.write(path, lines == null ? new ArrayList<>() : lines, StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            logger.error("写入文件行失败: " + filePath, e);
            return false;
        }
    }
    
    /**
     * 追加字符串到文件
     */
    public static boolean appendString(String filePath, String content) {
        if (StringUtil.isEmpty(filePath)) {
            return false;
        }
        
        try {
            Path path = Paths.get(filePath);
            Files.createDirectories(path.getParent());
            Files.writeString(path, content == null ? "" : content, StandardCharsets.UTF_8, 
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            return true;
        } catch (IOException e) {
            logger.error("追加文件失败: " + filePath, e);
            return false;
        }
    }
    
    /**
     * 获取文件大小
     */
    public static long getFileSize(String filePath) {
        if (StringUtil.isEmpty(filePath)) {
            return -1;
        }
        
        try {
            return Files.size(Paths.get(filePath));
        } catch (IOException e) {
            logger.error("获取文件大小失败: " + filePath, e);
            return -1;
        }
    }
    
    /**
     * 获取文件扩展名
     */
    public static String getFileExtension(String filePath) {
        if (StringUtil.isEmpty(filePath)) {
            return "";
        }
        
        int lastDotIndex = filePath.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filePath.length() - 1) {
            return "";
        }
        
        return filePath.substring(lastDotIndex + 1).toLowerCase();
    }
    
    /**
     * 获取文件名（不包含扩展名）
     */
    public static String getFileNameWithoutExtension(String filePath) {
        if (StringUtil.isEmpty(filePath)) {
            return "";
        }
        
        String fileName = Paths.get(filePath).getFileName().toString();
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return fileName;
        }
        
        return fileName.substring(0, lastDotIndex);
    }
    
    /**
     * 获取文件名（包含扩展名）
     */
    public static String getFileName(String filePath) {
        if (StringUtil.isEmpty(filePath)) {
            return "";
        }
        
        return Paths.get(filePath).getFileName().toString();
    }
    
    /**
     * 获取父目录路径
     */
    public static String getParentPath(String filePath) {
        if (StringUtil.isEmpty(filePath)) {
            return "";
        }
        
        Path parent = Paths.get(filePath).getParent();
        return parent == null ? "" : parent.toString();
    }
    
    /**
     * 列出目录中的所有文件
     */
    public static List<String> listFiles(String dirPath) {
        return listFiles(dirPath, false);
    }
    
    /**
     * 列出目录中的所有文件
     */
    public static List<String> listFiles(String dirPath, boolean recursive) {
        List<String> files = new ArrayList<>();
        
        if (StringUtil.isEmpty(dirPath) || !isDirectory(dirPath)) {
            return files;
        }
        
        try {
            Path dir = Paths.get(dirPath);
            if (recursive) {
                try (Stream<Path> paths = Files.walk(dir)) {
                    paths.filter(Files::isRegularFile)
                         .map(Path::toString)
                         .forEach(files::add);
                }
            } else {
                try (Stream<Path> paths = Files.list(dir)) {
                    paths.filter(Files::isRegularFile)
                         .map(Path::toString)
                         .forEach(files::add);
                }
            }
        } catch (IOException e) {
            logger.error("列出文件失败: " + dirPath, e);
        }
        
        return files;
    }
    
    /**
     * 列出目录中的所有子目录
     */
    public static List<String> listDirectories(String dirPath) {
        return listDirectories(dirPath, false);
    }
    
    /**
     * 列出目录中的所有子目录
     */
    public static List<String> listDirectories(String dirPath, boolean recursive) {
        List<String> directories = new ArrayList<>();
        
        if (StringUtil.isEmpty(dirPath) || !isDirectory(dirPath)) {
            return directories;
        }
        
        try {
            Path dir = Paths.get(dirPath);
            if (recursive) {
                try (Stream<Path> paths = Files.walk(dir)) {
                    paths.filter(Files::isDirectory)
                         .filter(path -> !path.equals(dir))
                         .map(Path::toString)
                         .forEach(directories::add);
                }
            } else {
                try (Stream<Path> paths = Files.list(dir)) {
                    paths.filter(Files::isDirectory)
                         .map(Path::toString)
                         .forEach(directories::add);
                }
            }
        } catch (IOException e) {
            logger.error("列出目录失败: " + dirPath, e);
        }
        
        return directories;
    }
    
    /**
     * 查找文件
     */
    public static List<String> findFiles(String dirPath, String pattern) {
        List<String> matchedFiles = new ArrayList<>();
        
        if (StringUtil.isEmpty(dirPath) || !isDirectory(dirPath)) {
            return matchedFiles;
        }
        
        try {
            Path dir = Paths.get(dirPath);
            PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
            
            try (Stream<Path> paths = Files.walk(dir)) {
                paths.filter(Files::isRegularFile)
                     .filter(path -> matcher.matches(path.getFileName()))
                     .map(Path::toString)
                     .forEach(matchedFiles::add);
            }
        } catch (IOException e) {
            logger.error("查找文件失败: " + dirPath + ", pattern: " + pattern, e);
        }
        
        return matchedFiles;
    }
    
    /**
     * 计算目录大小
     */
    public static long getDirectorySize(String dirPath) {
        if (StringUtil.isEmpty(dirPath) || !isDirectory(dirPath)) {
            return 0;
        }
        
        try {
            Path dir = Paths.get(dirPath);
            try (Stream<Path> paths = Files.walk(dir)) {
                return paths.filter(Files::isRegularFile)
                           .mapToLong(path -> {
                               try {
                                   return Files.size(path);
                               } catch (IOException e) {
                                   return 0;
                               }
                           })
                           .sum();
            }
        } catch (IOException e) {
            logger.error("计算目录大小失败: " + dirPath, e);
            return 0;
        }
    }
    
    /**
     * 格式化文件大小
     */
    public static String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        
        String[] units = {"KB", "MB", "GB", "TB"};
        double size = bytes;
        int unitIndex = -1;
        
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        
        return String.format("%.2f", size) + " " + units[unitIndex];
    }
    
    /**
     * 压缩文件到ZIP
     */
    public static boolean zipFiles(List<String> filePaths, String zipPath) {
        if (filePaths == null || filePaths.isEmpty() || StringUtil.isEmpty(zipPath)) {
            return false;
        }
        
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath))) {
            for (String filePath : filePaths) {
                if (!exists(filePath)) {
                    continue;
                }
                
                Path path = Paths.get(filePath);
                String entryName = path.getFileName().toString();
                
                ZipEntry entry = new ZipEntry(entryName);
                zos.putNextEntry(entry);
                
                Files.copy(path, zos);
                zos.closeEntry();
            }
            return true;
        } catch (IOException e) {
            logger.error("压缩文件失败: " + zipPath, e);
            return false;
        }
    }
    
    /**
     * 解压ZIP文件
     */
    public static boolean unzipFile(String zipPath, String targetDir) {
        if (StringUtil.isEmpty(zipPath) || StringUtil.isEmpty(targetDir) || !exists(zipPath)) {
            return false;
        }
        
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String filePath = targetDir + "/" + entry.getName();
                
                if (entry.isDirectory()) {
                    createDirectories(filePath);
                } else {
                    Path targetPath = Paths.get(filePath);
                    Files.createDirectories(targetPath.getParent());
                    Files.copy(zis, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
            return true;
        } catch (IOException e) {
            logger.error("解压文件失败: " + zipPath, e);
            return false;
        }
    }
    
    /**
     * 获取临时目录路径
     */
    public static String getTempDir() {
        return System.getProperty("java.io.tmpdir");
    }
    
    /**
     * 创建临时文件
     */
    public static String createTempFile(String prefix, String suffix) {
        try {
            Path tempFile = Files.createTempFile(prefix, suffix);
            return tempFile.toString();
        } catch (IOException e) {
            logger.error("创建临时文件失败", e);
            return null;
        }
    }
    
    /**
     * 清理临时文件
     */
    public static boolean cleanTempFiles(String prefix) {
        try {
            Path tempDir = Paths.get(getTempDir());
            try (Stream<Path> paths = Files.list(tempDir)) {
                paths.filter(path -> path.getFileName().toString().startsWith(prefix))
                     .forEach(path -> {
                         try {
                             Files.deleteIfExists(path);
                         } catch (IOException e) {
                             logger.warn("删除临时文件失败: " + path, e);
                         }
                     });
            }
            return true;
        } catch (IOException e) {
            logger.error("清理临时文件失败: " + prefix, e);
            return false;
        }
    }
}
