package net.csibio.propro.utils;

import org.apache.commons.io.FilenameUtils;

public class RepositoryUtil {

    /**
     * 项目文件夹仓库根目录
     */
    public static String PROJECT_ROOT = "Project";

    /**
     * 库文件夹根目录
     */
    public static String LIBRARY_ROOT = "Library";

    /**
     * 标准库文件夹根目录
     */
    public static String ANA_LIBRARY_ROOT = "Ana";

    /**
     * 内标库文件夹根目录
     */
    public static String INS_LIBRARY_ROOT = "Ins";

    /**
     * Fasta库文件夹根目录
     */
    public static String FASTA_LIBRARY_ROOT = "Fasta";

    /**
     * 文件导出文件夹
     */
    public static String EXPORT = "Export";

    public static String repository;

    public static String getRepo() {
        return repository;
    }

    public static String getProjectRoot() {
        return FilenameUtils.concat(repository, "Project");
    }

    public static String getProjectRepo(String projectName) {
        return FilenameUtils.concat(FilenameUtils.concat(repository, PROJECT_ROOT), projectName);
    }

    public static String getAnaLibraryRepo() {
        return FilenameUtils.concat(FilenameUtils.concat(repository, LIBRARY_ROOT), ANA_LIBRARY_ROOT);
    }

    public static String getIrtLibraryRepo() {
        return FilenameUtils.concat(FilenameUtils.concat(repository, LIBRARY_ROOT), INS_LIBRARY_ROOT);
    }

    public static String getFastaLibraryRepo() {
        return FilenameUtils.concat(FilenameUtils.concat(repository, LIBRARY_ROOT), FASTA_LIBRARY_ROOT);
    }

    public static String getExport(String projectName) {
        return FilenameUtils.concat(FilenameUtils.concat(repository, EXPORT), projectName);
    }

    public static String buildOutputPath(String projectName, String fileName) {
        String folderPath = FilenameUtils.concat(repository, projectName);
        return FilenameUtils.concat(folderPath, fileName);
    }
}
