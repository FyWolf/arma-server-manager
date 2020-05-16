package cz.forgottenempire.arma3servergui.cronjobs;

import cz.forgottenempire.arma3servergui.Constants;
import cz.forgottenempire.arma3servergui.model.SteamAuth;
import cz.forgottenempire.arma3servergui.services.JsonDbService;
import cz.forgottenempire.arma3servergui.services.WorkshopInstallerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class UpdateModsCronJob {
    private WorkshopInstallerService workshopInstallerService;
    private JsonDbService<SteamAuth> steamAuthDb;

    public UpdateModsCronJob() {
        log.info("Scheduling mod update cronjob for 03:00 AM every day");
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void refreshMods() {
        log.info("Running update job");
        SteamAuth auth = steamAuthDb.find(Constants.ACCOUND_DEFAULT_ID, SteamAuth.class);
        if (auth != null && auth.getUsername() != null && auth.getPassword() != null) {
            workshopInstallerService.updateAllMods(auth);
            log.info("Update job finished");
        } else {
            log.warn("Could not finish update job, no auth for steam workshop given");
        }
    }

    @Autowired
    public void setWorkshopInstallerService(WorkshopInstallerService workshopInstallerService) {
        this.workshopInstallerService = workshopInstallerService;
    }

    @Autowired
    public void setSteamAuthDb(JsonDbService<SteamAuth> steamAuthDb) {
        this.steamAuthDb = steamAuthDb;
    }
}
