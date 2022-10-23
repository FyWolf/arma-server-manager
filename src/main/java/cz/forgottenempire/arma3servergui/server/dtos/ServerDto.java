package cz.forgottenempire.arma3servergui.server.dtos;

import cz.forgottenempire.arma3servergui.server.entities.Server.ServerType;
import cz.forgottenempire.arma3servergui.workshop.dtos.CreatorDlcDto;
import java.util.List;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ServerDto {
    private Long id;
    @NotEmpty
    private String name;
    private String description;
    @Min(1)
    private int port;
    @Min(1)
    private int queryPort;
    @Min(1)
    private int maxPlayers;

    @NotNull(message = "must be filled in. Available types: [ARMA3, ARMA4, DAYZ, REFORGER]")
    private ServerType type;

    private String password;
    private String adminPassword;

    private boolean clientFilePatching;
    private boolean serverFilePatching;
    private boolean persistent;
    private boolean battlEye;
    private boolean von;
    private boolean verifySignatures;

    private String additionalOptions;

    private List<ServerWorkshopModDto> activeMods;
    private List<CreatorDlcDto> activeDLCs;

    private ServerInstanceInfoDto instanceInfo;
}