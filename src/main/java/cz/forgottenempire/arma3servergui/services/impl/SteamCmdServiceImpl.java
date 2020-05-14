package cz.forgottenempire.arma3servergui.services.impl;

import cz.forgottenempire.arma3servergui.Constants;
import cz.forgottenempire.arma3servergui.model.SteamAuth;
import cz.forgottenempire.arma3servergui.model.WorkshopMod;
import cz.forgottenempire.arma3servergui.services.JsonDbService;
import cz.forgottenempire.arma3servergui.services.SteamCmdService;
import cz.forgottenempire.arma3servergui.services.SteamWorkshopService;
import cz.forgottenempire.arma3servergui.util.SteamCmdWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SteamCmdServiceImpl implements SteamCmdService {
    @Value("${installDir}")
    private String downloadPath;

    @Value("${serverDir}")
    private String serverPath;

    private JsonDbService<WorkshopMod> modDb;
    private SteamCmdWrapper steamCmd;
    private SteamWorkshopService steamWorkshopService;

    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1,
            0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());

    @Override
    public boolean installOrUpdateMod(SteamAuth auth, WorkshopMod mod) {
        executor.submit(() -> {
            mod.setInstalled(false);
            modDb.save(mod, WorkshopMod.class);

            log.info("Starting download of mod {} (id {})", mod.getName(), mod.getId());

            if (!downloadMod(auth, mod.getId())) {
                log.error("Failed to download mod {} ({}) ", mod.getName(), mod.getId());
            }

            copyBiKeys(mod.getId());
            createSymlink(mod);

            mod.setInstalled(true);
            SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            mod.setLastUpdated(formatter.format(new Date()));
            mod.setFileSize(steamWorkshopService.getFileSize(mod.getId()));
            modDb.save(mod, WorkshopMod.class);

            log.info("Mod {} (id {}) successfully installed ({} left in queue)", mod.getName(), mod.getId(),
                    executor.getQueue().size());
        });

        return true;
    }

    @Override
    public boolean deleteMod(WorkshopMod mod) {
        File modDirectory = new File(getDownloadPath() + File.separatorChar + mod.getId());
        try {
            FileUtils.deleteDirectory(modDirectory);
            deleteSymlink(mod);
            modDb.remove(mod, WorkshopMod.class);
        } catch (IOException e) {
            log.error("Could not delete directory {} due to {}", modDirectory, e.toString());
            return false;
        }
        log.info("Mod {} ({}) successfully deleted", mod.getName(), mod.getId());
        return true;
    }

    @Override
    public boolean refreshMods(SteamAuth auth) {
        File modDirectory = new File(getDownloadPath());

        try {
            // find all installed mods in installation folder
            Set<Long> modIds = Files.list(modDirectory.toPath())
                    .map(p -> {
                        try {
                            return Long.parseLong(p.getFileName().toString());
                        } catch (NumberFormatException e) {
                            return null;
                        }
                    })
                    .collect(Collectors.toSet());

            // add all missing mods from db
            modDb.findAll(WorkshopMod.class).stream()
                    .peek(mod -> {
                        mod.setInstalled(false);
                        modDb.save(mod, WorkshopMod.class);
                    })
                    .map(WorkshopMod::getId)
                    .forEach(modIds::add);

            // refresh/update all found mods
            for (Long modId : modIds) {
                WorkshopMod mod = modDb.find(modId, WorkshopMod.class);
                if (mod == null) {
                    // create mods which were not persisted in db
                    mod = new WorkshopMod(modId);
                    mod.setName(steamWorkshopService.getModName(modId));
                    modDb.save(mod, WorkshopMod.class);
                }

                installOrUpdateMod(auth, mod);
            }
        } catch (IOException e) {
            log.error("Failed to refresh mods due to {}", e.toString());
            return false;
        }

        return true;
    }

    private boolean downloadMod(SteamAuth auth, Long modId) {
        List<String> args = new ArrayList<>();
        args.add("+@NoPromptForPassword 1");
        args.add("+@ShutdownOnFailedCommand 0");

        String token = auth.getSteamGuardToken();
        if (token != null && !token.isBlank()) {
            args.add("+set_steam_guard_code " + token);
        }

        args.add("+login " + auth.getUsername() + " " + auth.getPassword());
        args.add("+force_install_dir " + downloadPath);
        args.add("+workshop_download_item " + Constants.STEAM_ARMA3_ID + " " + modId + " validate");
        args.add("+quit");

        boolean success = false;
        int attempts = 0;
        while (!success && attempts++ <= 10) {
            try {
                log.info("Starting mod download, attempt {} / 10", attempts);
                success = steamCmd.execute(args);
            } catch (IOException | InterruptedException e) {
                log.error("SteamCmd execution failed due to {}", e.toString());
            }
        }

        if (success) {
            log.info("SteamCMD successfully returned");
        } else {
            log.error("SteamCMD failed");
        }

        return success;
    }

    private void createSymlink(WorkshopMod mod) {
        // create symlink to server
        Path linkPath = Path.of(serverPath + File.separatorChar + mod.getNormalizedName());
        Path targetPath = Path.of(getDownloadPath() + File.separatorChar + mod.getId());

        log.info("Creating symlink - link {}, target {}", linkPath, targetPath);

        try {
            if (!Files.isSymbolicLink(linkPath)) {
                Files.createSymbolicLink(linkPath, targetPath);
            }
        } catch (IOException e) {
            log.error("Failed to create symlink for mod {} ({}) due to {} ",
                    mod.getName(), mod.getId(), e.toString());
        }
    }

    private void deleteSymlink(WorkshopMod mod) {
        Path linkPath = Path.of(serverPath + File.separatorChar + mod.getNormalizedName());
        log.info("Deleting symlink {}", linkPath);
        try {
            Files.delete(linkPath);
        } catch (IOException e) {
            log.warn("Could not delete symlink {} due to {}", linkPath, e.toString());
        }
    }

    private void copyBiKeys(Long modId) {
        String[] extensions = new String[]{"bikey"};
        File modDirectory = new File(getDownloadPath() + File.separatorChar + modId);
        if (!modDirectory.isDirectory()) {
            log.error("Can not access mod directory {}", modId);
            return;
        }

        for (Iterator<File> it = FileUtils.iterateFiles(modDirectory, extensions, true); it.hasNext(); ) {
            File key = it.next();
            try {
                log.info("Copying bikey {} to server", key.getName());
                FileUtils.copyFile(key, new File(serverPath + File.separatorChar + "keys" + File.separatorChar + key.getName()));
            } catch (IOException e) {
                log.error("Could not copy bikeys due to {}", e.toString());
            }
        }
    }

    private String getDownloadPath() {
        return downloadPath
                + File.separatorChar + "steamapps"
                + File.separatorChar + "workshop"
                + File.separatorChar + "content"
                + File.separatorChar + Constants.STEAM_ARMA3_ID;
    }

    @Autowired
    public void setSteamCmd(SteamCmdWrapper steamCmd) {
        this.steamCmd = steamCmd;
    }

    @Autowired
    public void setModDb(JsonDbService<WorkshopMod> modDb) {
        this.modDb = modDb;
    }

    @Autowired
    public void setSteamWorkshopService(SteamWorkshopService steamWorkshopService) {
        this.steamWorkshopService = steamWorkshopService;
    }
}
