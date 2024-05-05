package cz.forgottenempire.servermanager.installation;

import cz.forgottenempire.servermanager.common.InstallationStatus;
import cz.forgottenempire.servermanager.common.ServerType;
import cz.forgottenempire.servermanager.common.exceptions.NotFoundException;
import cz.forgottenempire.servermanager.util.SystemUtils;
import cz.forgottenempire.servermanager.util.SystemUtils.OSType;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ServerInstallationService {

    private final ServerInstallationRepository installationRepository;

    @Autowired
    public ServerInstallationService(ServerInstallationRepository installationRepository) {
        this.installationRepository = installationRepository;
    }

    public List<ServerInstallation> getAvailableServerInstallations() {
        return getAvailableServerTypes().stream()
                .map(this::getServerInstallation)
                .toList();
    }

    public ServerInstallation getServerInstallation(ServerType type) {
        return installationRepository.findById(type)
                .orElseThrow(() -> new NotFoundException("Server of type '" + type + "' not found."));
    }

    public boolean isServerInstalled(ServerType type) {
        return getServerInstallation(type).getInstallationStatus() == InstallationStatus.FINISHED;
    }

    private List<ServerType> getAvailableServerTypes() {
        return ServerType.getAll();
    }
}
