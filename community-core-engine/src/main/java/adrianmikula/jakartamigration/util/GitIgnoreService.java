package adrianmikula.jakartamigration.util;

import nl.basjes.gitignore.GitIgnoreFileSet;
import java.nio.file.Path;
import java.io.File;

/**
 * Wrapper around gitignore-reader's GitIgnoreFileSet.
 * Provides simple boolean check for path exclusion.
 */
public class GitIgnoreService {
    private final GitIgnoreFileSet gitIgnoreFileSet;
    private final Path projectRoot;
    
    public GitIgnoreService(Path projectRoot) {
        this.projectRoot = projectRoot;
        this.gitIgnoreFileSet = new GitIgnoreFileSet(projectRoot.toFile());
    }
    
    /**
     * Checks if the given path (relative to project root) is ignored by .gitignore.
     * @param relativePath path relative to project root
     * @return true if ignored, false otherwise
     */
    public boolean isIgnored(Path relativePath) {
        // GitIgnoreFileSet expects a String path using system separators
        return gitIgnoreFileSet.ignoreFile(relativePath.toString());
    }
    
    /**
     * Checks if the given absolute path is ignored.
     * @param absolutePath absolute path within the project
     * @return true if ignored, false otherwise
     */
    public boolean isIgnoredAbsolute(Path absolutePath) {
        Path relative = projectRoot.relativize(absolutePath);
        return isIgnored(relative);
    }
}
