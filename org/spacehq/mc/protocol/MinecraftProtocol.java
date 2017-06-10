package org.spacehq.mc.protocol;

import java.net.Proxy;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.util.UUID;
import org.spacehq.mc.auth.data.GameProfile;
import org.spacehq.mc.auth.exception.request.RequestException;
import org.spacehq.mc.auth.service.AuthenticationService;
import org.spacehq.mc.protocol.data.SubProtocol;
import org.spacehq.mc.protocol.packet.handshake.client.HandshakePacket;
import org.spacehq.mc.protocol.packet.ingame.client.ClientChatPacket;
import org.spacehq.mc.protocol.packet.ingame.client.ClientKeepAlivePacket;
import org.spacehq.mc.protocol.packet.ingame.client.ClientPluginMessagePacket;
import org.spacehq.mc.protocol.packet.ingame.client.ClientRequestPacket;
import org.spacehq.mc.protocol.packet.ingame.client.ClientResourcePackStatusPacket;
import org.spacehq.mc.protocol.packet.ingame.client.ClientSettingsPacket;
import org.spacehq.mc.protocol.packet.ingame.client.ClientTabCompletePacket;
import org.spacehq.mc.protocol.packet.ingame.client.player.ClientChangeHeldItemPacket;
import org.spacehq.mc.protocol.packet.ingame.client.player.ClientPlayerAbilitiesPacket;
import org.spacehq.mc.protocol.packet.ingame.client.player.ClientPlayerActionPacket;
import org.spacehq.mc.protocol.packet.ingame.client.player.ClientPlayerInteractEntityPacket;
import org.spacehq.mc.protocol.packet.ingame.client.player.ClientPlayerMovementPacket;
import org.spacehq.mc.protocol.packet.ingame.client.player.ClientPlayerPlaceBlockPacket;
import org.spacehq.mc.protocol.packet.ingame.client.player.ClientPlayerPositionPacket;
import org.spacehq.mc.protocol.packet.ingame.client.player.ClientPlayerPositionRotationPacket;
import org.spacehq.mc.protocol.packet.ingame.client.player.ClientPlayerRotationPacket;
import org.spacehq.mc.protocol.packet.ingame.client.player.ClientPlayerStatePacket;
import org.spacehq.mc.protocol.packet.ingame.client.player.ClientSpectatePacket;
import org.spacehq.mc.protocol.packet.ingame.client.player.ClientSteerVehiclePacket;
import org.spacehq.mc.protocol.packet.ingame.client.player.ClientSwingArmPacket;
import org.spacehq.mc.protocol.packet.ingame.client.window.ClientCloseWindowPacket;
import org.spacehq.mc.protocol.packet.ingame.client.window.ClientConfirmTransactionPacket;
import org.spacehq.mc.protocol.packet.ingame.client.window.ClientCreativeInventoryActionPacket;
import org.spacehq.mc.protocol.packet.ingame.client.window.ClientEnchantItemPacket;
import org.spacehq.mc.protocol.packet.ingame.client.window.ClientWindowActionPacket;
import org.spacehq.mc.protocol.packet.ingame.client.world.ClientUpdateSignPacket;
import org.spacehq.mc.protocol.packet.ingame.server.ServerChatPacket;
import org.spacehq.mc.protocol.packet.ingame.server.ServerCombatPacket;
import org.spacehq.mc.protocol.packet.ingame.server.ServerDifficultyPacket;
import org.spacehq.mc.protocol.packet.ingame.server.ServerDisconnectPacket;
import org.spacehq.mc.protocol.packet.ingame.server.ServerJoinGamePacket;
import org.spacehq.mc.protocol.packet.ingame.server.ServerKeepAlivePacket;
import org.spacehq.mc.protocol.packet.ingame.server.ServerPlayerListDataPacket;
import org.spacehq.mc.protocol.packet.ingame.server.ServerPlayerListEntryPacket;
import org.spacehq.mc.protocol.packet.ingame.server.ServerPluginMessagePacket;
import org.spacehq.mc.protocol.packet.ingame.server.ServerResourcePackSendPacket;
import org.spacehq.mc.protocol.packet.ingame.server.ServerRespawnPacket;
import org.spacehq.mc.protocol.packet.ingame.server.ServerSetCompressionPacket;
import org.spacehq.mc.protocol.packet.ingame.server.ServerStatisticsPacket;
import org.spacehq.mc.protocol.packet.ingame.server.ServerSwitchCameraPacket;
import org.spacehq.mc.protocol.packet.ingame.server.ServerTabCompletePacket;
import org.spacehq.mc.protocol.packet.ingame.server.ServerTitlePacket;
import org.spacehq.mc.protocol.packet.ingame.server.entity.ServerAnimationPacket;
import org.spacehq.mc.protocol.packet.ingame.server.entity.ServerCollectItemPacket;
import org.spacehq.mc.protocol.packet.ingame.server.entity.ServerDestroyEntitiesPacket;
import org.spacehq.mc.protocol.packet.ingame.server.entity.ServerEntityAttachPacket;
import org.spacehq.mc.protocol.packet.ingame.server.entity.ServerEntityEffectPacket;
import org.spacehq.mc.protocol.packet.ingame.server.entity.ServerEntityEquipmentPacket;
import org.spacehq.mc.protocol.packet.ingame.server.entity.ServerEntityHeadLookPacket;
import org.spacehq.mc.protocol.packet.ingame.server.entity.ServerEntityMetadataPacket;
import org.spacehq.mc.protocol.packet.ingame.server.entity.ServerEntityMovementPacket;
import org.spacehq.mc.protocol.packet.ingame.server.entity.ServerEntityNBTUpdatePacket;
import org.spacehq.mc.protocol.packet.ingame.server.entity.ServerEntityPositionPacket;
import org.spacehq.mc.protocol.packet.ingame.server.entity.ServerEntityPositionRotationPacket;
import org.spacehq.mc.protocol.packet.ingame.server.entity.ServerEntityPropertiesPacket;
import org.spacehq.mc.protocol.packet.ingame.server.entity.ServerEntityRemoveEffectPacket;
import org.spacehq.mc.protocol.packet.ingame.server.entity.ServerEntityRotationPacket;
import org.spacehq.mc.protocol.packet.ingame.server.entity.ServerEntityStatusPacket;
import org.spacehq.mc.protocol.packet.ingame.server.entity.ServerEntityTeleportPacket;
import org.spacehq.mc.protocol.packet.ingame.server.entity.ServerEntityVelocityPacket;
import org.spacehq.mc.protocol.packet.ingame.server.entity.player.ServerChangeHeldItemPacket;
import org.spacehq.mc.protocol.packet.ingame.server.entity.player.ServerPlayerAbilitiesPacket;
import org.spacehq.mc.protocol.packet.ingame.server.entity.player.ServerPlayerPositionRotationPacket;
import org.spacehq.mc.protocol.packet.ingame.server.entity.player.ServerPlayerUseBedPacket;
import org.spacehq.mc.protocol.packet.ingame.server.entity.player.ServerSetExperiencePacket;
import org.spacehq.mc.protocol.packet.ingame.server.entity.player.ServerUpdateHealthPacket;
import org.spacehq.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnExpOrbPacket;
import org.spacehq.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnGlobalEntityPacket;
import org.spacehq.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnMobPacket;
import org.spacehq.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnObjectPacket;
import org.spacehq.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnPaintingPacket;
import org.spacehq.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnPlayerPacket;
import org.spacehq.mc.protocol.packet.ingame.server.scoreboard.ServerDisplayScoreboardPacket;
import org.spacehq.mc.protocol.packet.ingame.server.scoreboard.ServerScoreboardObjectivePacket;
import org.spacehq.mc.protocol.packet.ingame.server.scoreboard.ServerTeamPacket;
import org.spacehq.mc.protocol.packet.ingame.server.scoreboard.ServerUpdateScorePacket;
import org.spacehq.mc.protocol.packet.ingame.server.window.ServerCloseWindowPacket;
import org.spacehq.mc.protocol.packet.ingame.server.window.ServerConfirmTransactionPacket;
import org.spacehq.mc.protocol.packet.ingame.server.window.ServerOpenWindowPacket;
import org.spacehq.mc.protocol.packet.ingame.server.window.ServerSetSlotPacket;
import org.spacehq.mc.protocol.packet.ingame.server.window.ServerWindowItemsPacket;
import org.spacehq.mc.protocol.packet.ingame.server.window.ServerWindowPropertyPacket;
import org.spacehq.mc.protocol.packet.ingame.server.world.ServerBlockBreakAnimPacket;
import org.spacehq.mc.protocol.packet.ingame.server.world.ServerBlockChangePacket;
import org.spacehq.mc.protocol.packet.ingame.server.world.ServerBlockValuePacket;
import org.spacehq.mc.protocol.packet.ingame.server.world.ServerChunkDataPacket;
import org.spacehq.mc.protocol.packet.ingame.server.world.ServerExplosionPacket;
import org.spacehq.mc.protocol.packet.ingame.server.world.ServerMapDataPacket;
import org.spacehq.mc.protocol.packet.ingame.server.world.ServerMultiBlockChangePacket;
import org.spacehq.mc.protocol.packet.ingame.server.world.ServerMultiChunkDataPacket;
import org.spacehq.mc.protocol.packet.ingame.server.world.ServerNotifyClientPacket;
import org.spacehq.mc.protocol.packet.ingame.server.world.ServerOpenTileEntityEditorPacket;
import org.spacehq.mc.protocol.packet.ingame.server.world.ServerPlayEffectPacket;
import org.spacehq.mc.protocol.packet.ingame.server.world.ServerPlaySoundPacket;
import org.spacehq.mc.protocol.packet.ingame.server.world.ServerSpawnParticlePacket;
import org.spacehq.mc.protocol.packet.ingame.server.world.ServerSpawnPositionPacket;
import org.spacehq.mc.protocol.packet.ingame.server.world.ServerUpdateSignPacket;
import org.spacehq.mc.protocol.packet.ingame.server.world.ServerUpdateTileEntityPacket;
import org.spacehq.mc.protocol.packet.ingame.server.world.ServerUpdateTimePacket;
import org.spacehq.mc.protocol.packet.ingame.server.world.ServerWorldBorderPacket;
import org.spacehq.mc.protocol.packet.login.client.EncryptionResponsePacket;
import org.spacehq.mc.protocol.packet.login.client.LoginStartPacket;
import org.spacehq.mc.protocol.packet.login.server.EncryptionRequestPacket;
import org.spacehq.mc.protocol.packet.login.server.LoginDisconnectPacket;
import org.spacehq.mc.protocol.packet.login.server.LoginSetCompressionPacket;
import org.spacehq.mc.protocol.packet.login.server.LoginSuccessPacket;
import org.spacehq.mc.protocol.packet.status.client.StatusPingPacket;
import org.spacehq.mc.protocol.packet.status.client.StatusQueryPacket;
import org.spacehq.mc.protocol.packet.status.server.StatusPongPacket;
import org.spacehq.mc.protocol.packet.status.server.StatusResponsePacket;
import org.spacehq.packetlib.Client;
import org.spacehq.packetlib.Server;
import org.spacehq.packetlib.Session;
import org.spacehq.packetlib.crypt.AESEncryption;
import org.spacehq.packetlib.crypt.PacketEncryption;
import org.spacehq.packetlib.packet.DefaultPacketHeader;
import org.spacehq.packetlib.packet.PacketHeader;
import org.spacehq.packetlib.packet.PacketProtocol;

public class MinecraftProtocol
  extends PacketProtocol
{
  private SubProtocol subProtocol = SubProtocol.HANDSHAKE;
  private PacketHeader header = new DefaultPacketHeader();
  private AESEncryption encrypt;
  private GameProfile profile;
  private String accessToken = "";
  
  private MinecraftProtocol() {}
  
  public MinecraftProtocol(SubProtocol subProtocol)
  {
    if ((subProtocol != SubProtocol.LOGIN) && (subProtocol != SubProtocol.STATUS)) {
      throw new IllegalArgumentException("Only login and status modes are permitted.");
    }
    this.subProtocol = subProtocol;
    if (subProtocol == SubProtocol.LOGIN) {
      this.profile = new GameProfile((UUID)null, "Player");
    }
  }
  
  public MinecraftProtocol(String username)
  {
    this(SubProtocol.LOGIN);
    this.profile = new GameProfile((UUID)null, username);
  }
  
  public MinecraftProtocol(String username, String password)
    throws RequestException
  {
    this(username, password, false);
  }
  
  public MinecraftProtocol(String username, String using, boolean token)
    throws RequestException
  {
    this(username, using, token, Proxy.NO_PROXY);
  }
  
  public MinecraftProtocol(String username, String using, boolean token, Proxy authProxy)
    throws RequestException
  {
    this(SubProtocol.LOGIN);
    String clientToken = UUID.randomUUID().toString();
    AuthenticationService auth = new AuthenticationService(clientToken, authProxy);
    auth.setUsername(username);
    if (token) {
      auth.setAccessToken(using);
    } else {
      auth.setPassword(using);
    }
    auth.login();
    this.profile = auth.getSelectedProfile();
    this.accessToken = auth.getAccessToken();
  }
  
  public MinecraftProtocol(GameProfile profile, String accessToken)
  {
    this(SubProtocol.LOGIN);
    this.profile = profile;
    this.accessToken = accessToken;
  }
  
  public GameProfile getProfile()
  {
    return this.profile;
  }
  
  public String getAccessToken()
  {
    return this.accessToken;
  }
  
  public String getSRVRecordPrefix()
  {
    return "_minecraft";
  }
  
  public PacketHeader getPacketHeader()
  {
    return this.header;
  }
  
  public PacketEncryption getEncryption()
  {
    return this.encrypt;
  }
  
  public void newClientSession(Client client, Session session)
  {
    if (this.profile != null)
    {
      session.setFlag("profile", this.profile);
      session.setFlag("access-token", this.accessToken);
    }
    setSubProtocol(this.subProtocol, true, session);
    session.addListener(new ClientListener());
  }
  
  public void newServerSession(Server server, Session session)
  {
    setSubProtocol(SubProtocol.HANDSHAKE, false, session);
    session.addListener(new ServerListener());
  }
  
  protected void enableEncryption(Key key)
  {
    try
    {
      this.encrypt = new AESEncryption(key);
    }
    catch (GeneralSecurityException e)
    {
      throw new Error("Failed to enable protocol encryption.", e);
    }
  }
  
  public SubProtocol getSubProtocol()
  {
    return this.subProtocol;
  }
  
  protected void setSubProtocol(SubProtocol subProtocol, boolean client, Session session)
  {
    clearPackets();
    switch (subProtocol)
    {
    case HANDSHAKE: 
      if (client) {
        initClientHandshake(session);
      } else {
        initServerHandshake(session);
      }
      break;
    case LOGIN: 
      if (client) {
        initClientLogin(session);
      } else {
        initServerLogin(session);
      }
      break;
    case GAME: 
      if (client) {
        initClientGame(session);
      } else {
        initServerGame(session);
      }
      break;
    case STATUS: 
      if (client) {
        initClientStatus(session);
      } else {
        initServerStatus(session);
      }
      break;
    }
    this.subProtocol = subProtocol;
  }
  
  private void initClientHandshake(Session session)
  {
    registerOutgoing(0, HandshakePacket.class);
  }
  
  private void initServerHandshake(Session session)
  {
    registerIncoming(0, HandshakePacket.class);
  }
  
  private void initClientLogin(Session session)
  {
    registerIncoming(0, LoginDisconnectPacket.class);
    registerIncoming(1, EncryptionRequestPacket.class);
    registerIncoming(2, LoginSuccessPacket.class);
    registerIncoming(3, LoginSetCompressionPacket.class);
    
    registerOutgoing(0, LoginStartPacket.class);
    registerOutgoing(1, EncryptionResponsePacket.class);
  }
  
  private void initServerLogin(Session session)
  {
    registerIncoming(0, LoginStartPacket.class);
    registerIncoming(1, EncryptionResponsePacket.class);
    
    registerOutgoing(0, LoginDisconnectPacket.class);
    registerOutgoing(1, EncryptionRequestPacket.class);
    registerOutgoing(2, LoginSuccessPacket.class);
    registerOutgoing(3, LoginSetCompressionPacket.class);
  }
  
  private void initClientGame(Session session)
  {
    registerIncoming(0, ServerKeepAlivePacket.class);
    registerIncoming(1, ServerJoinGamePacket.class);
    registerIncoming(2, ServerChatPacket.class);
    registerIncoming(3, ServerUpdateTimePacket.class);
    registerIncoming(4, ServerEntityEquipmentPacket.class);
    registerIncoming(5, ServerSpawnPositionPacket.class);
    registerIncoming(6, ServerUpdateHealthPacket.class);
    registerIncoming(7, ServerRespawnPacket.class);
    registerIncoming(8, ServerPlayerPositionRotationPacket.class);
    registerIncoming(9, ServerChangeHeldItemPacket.class);
    registerIncoming(10, ServerPlayerUseBedPacket.class);
    registerIncoming(11, ServerAnimationPacket.class);
    registerIncoming(12, ServerSpawnPlayerPacket.class);
    registerIncoming(13, ServerCollectItemPacket.class);
    registerIncoming(14, ServerSpawnObjectPacket.class);
    registerIncoming(15, ServerSpawnMobPacket.class);
    registerIncoming(16, ServerSpawnPaintingPacket.class);
    registerIncoming(17, ServerSpawnExpOrbPacket.class);
    registerIncoming(18, ServerEntityVelocityPacket.class);
    registerIncoming(19, ServerDestroyEntitiesPacket.class);
    registerIncoming(20, ServerEntityMovementPacket.class);
    registerIncoming(21, ServerEntityPositionPacket.class);
    registerIncoming(22, ServerEntityRotationPacket.class);
    registerIncoming(23, ServerEntityPositionRotationPacket.class);
    registerIncoming(24, ServerEntityTeleportPacket.class);
    registerIncoming(25, ServerEntityHeadLookPacket.class);
    registerIncoming(26, ServerEntityStatusPacket.class);
    registerIncoming(27, ServerEntityAttachPacket.class);
    registerIncoming(28, ServerEntityMetadataPacket.class);
    registerIncoming(29, ServerEntityEffectPacket.class);
    registerIncoming(30, ServerEntityRemoveEffectPacket.class);
    registerIncoming(31, ServerSetExperiencePacket.class);
    registerIncoming(32, ServerEntityPropertiesPacket.class);
    registerIncoming(33, ServerChunkDataPacket.class);
    registerIncoming(34, ServerMultiBlockChangePacket.class);
    registerIncoming(35, ServerBlockChangePacket.class);
    registerIncoming(36, ServerBlockValuePacket.class);
    registerIncoming(37, ServerBlockBreakAnimPacket.class);
    registerIncoming(38, ServerMultiChunkDataPacket.class);
    registerIncoming(39, ServerExplosionPacket.class);
    registerIncoming(40, ServerPlayEffectPacket.class);
    registerIncoming(41, ServerPlaySoundPacket.class);
    registerIncoming(42, ServerSpawnParticlePacket.class);
    registerIncoming(43, ServerNotifyClientPacket.class);
    registerIncoming(44, ServerSpawnGlobalEntityPacket.class);
    registerIncoming(45, ServerOpenWindowPacket.class);
    registerIncoming(46, ServerCloseWindowPacket.class);
    registerIncoming(47, ServerSetSlotPacket.class);
    registerIncoming(48, ServerWindowItemsPacket.class);
    registerIncoming(49, ServerWindowPropertyPacket.class);
    registerIncoming(50, ServerConfirmTransactionPacket.class);
    registerIncoming(51, ServerUpdateSignPacket.class);
    registerIncoming(52, ServerMapDataPacket.class);
    registerIncoming(53, ServerUpdateTileEntityPacket.class);
    registerIncoming(54, ServerOpenTileEntityEditorPacket.class);
    registerIncoming(55, ServerStatisticsPacket.class);
    registerIncoming(56, ServerPlayerListEntryPacket.class);
    registerIncoming(57, ServerPlayerAbilitiesPacket.class);
    registerIncoming(58, ServerTabCompletePacket.class);
    registerIncoming(59, ServerScoreboardObjectivePacket.class);
    registerIncoming(60, ServerUpdateScorePacket.class);
    registerIncoming(61, ServerDisplayScoreboardPacket.class);
    registerIncoming(62, ServerTeamPacket.class);
    registerIncoming(63, ServerPluginMessagePacket.class);
    registerIncoming(64, ServerDisconnectPacket.class);
    registerIncoming(65, ServerDifficultyPacket.class);
    registerIncoming(66, ServerCombatPacket.class);
    registerIncoming(67, ServerSwitchCameraPacket.class);
    registerIncoming(68, ServerWorldBorderPacket.class);
    registerIncoming(69, ServerTitlePacket.class);
    registerIncoming(70, ServerSetCompressionPacket.class);
    registerIncoming(71, ServerPlayerListDataPacket.class);
    registerIncoming(72, ServerResourcePackSendPacket.class);
    registerIncoming(73, ServerEntityNBTUpdatePacket.class);
    
    registerOutgoing(0, ClientKeepAlivePacket.class);
    registerOutgoing(1, ClientChatPacket.class);
    registerOutgoing(2, ClientPlayerInteractEntityPacket.class);
    registerOutgoing(3, ClientPlayerMovementPacket.class);
    registerOutgoing(4, ClientPlayerPositionPacket.class);
    registerOutgoing(5, ClientPlayerRotationPacket.class);
    registerOutgoing(6, ClientPlayerPositionRotationPacket.class);
    registerOutgoing(7, ClientPlayerActionPacket.class);
    registerOutgoing(8, ClientPlayerPlaceBlockPacket.class);
    registerOutgoing(9, ClientChangeHeldItemPacket.class);
    registerOutgoing(10, ClientSwingArmPacket.class);
    registerOutgoing(11, ClientPlayerStatePacket.class);
    registerOutgoing(12, ClientSteerVehiclePacket.class);
    registerOutgoing(13, ClientCloseWindowPacket.class);
    registerOutgoing(14, ClientWindowActionPacket.class);
    registerOutgoing(15, ClientConfirmTransactionPacket.class);
    registerOutgoing(16, ClientCreativeInventoryActionPacket.class);
    registerOutgoing(17, ClientEnchantItemPacket.class);
    registerOutgoing(18, ClientUpdateSignPacket.class);
    registerOutgoing(19, ClientPlayerAbilitiesPacket.class);
    registerOutgoing(20, ClientTabCompletePacket.class);
    registerOutgoing(21, ClientSettingsPacket.class);
    registerOutgoing(22, ClientRequestPacket.class);
    registerOutgoing(23, ClientPluginMessagePacket.class);
    registerOutgoing(24, ClientSpectatePacket.class);
    registerOutgoing(25, ClientResourcePackStatusPacket.class);
  }
  
  private void initServerGame(Session session)
  {
    registerIncoming(0, ClientKeepAlivePacket.class);
    registerIncoming(1, ClientChatPacket.class);
    registerIncoming(2, ClientPlayerInteractEntityPacket.class);
    registerIncoming(3, ClientPlayerMovementPacket.class);
    registerIncoming(4, ClientPlayerPositionPacket.class);
    registerIncoming(5, ClientPlayerRotationPacket.class);
    registerIncoming(6, ClientPlayerPositionRotationPacket.class);
    registerIncoming(7, ClientPlayerActionPacket.class);
    registerIncoming(8, ClientPlayerPlaceBlockPacket.class);
    registerIncoming(9, ClientChangeHeldItemPacket.class);
    registerIncoming(10, ClientSwingArmPacket.class);
    registerIncoming(11, ClientPlayerStatePacket.class);
    registerIncoming(12, ClientSteerVehiclePacket.class);
    registerIncoming(13, ClientCloseWindowPacket.class);
    registerIncoming(14, ClientWindowActionPacket.class);
    registerIncoming(15, ClientConfirmTransactionPacket.class);
    registerIncoming(16, ClientCreativeInventoryActionPacket.class);
    registerIncoming(17, ClientEnchantItemPacket.class);
    registerIncoming(18, ClientUpdateSignPacket.class);
    registerIncoming(19, ClientPlayerAbilitiesPacket.class);
    registerIncoming(20, ClientTabCompletePacket.class);
    registerIncoming(21, ClientSettingsPacket.class);
    registerIncoming(22, ClientRequestPacket.class);
    registerIncoming(23, ClientPluginMessagePacket.class);
    registerIncoming(24, ClientSpectatePacket.class);
    registerIncoming(25, ClientResourcePackStatusPacket.class);
    
    registerOutgoing(0, ServerKeepAlivePacket.class);
    registerOutgoing(1, ServerJoinGamePacket.class);
    registerOutgoing(2, ServerChatPacket.class);
    registerOutgoing(3, ServerUpdateTimePacket.class);
    registerOutgoing(4, ServerEntityEquipmentPacket.class);
    registerOutgoing(5, ServerSpawnPositionPacket.class);
    registerOutgoing(6, ServerUpdateHealthPacket.class);
    registerOutgoing(7, ServerRespawnPacket.class);
    registerOutgoing(8, ServerPlayerPositionRotationPacket.class);
    registerOutgoing(9, ServerChangeHeldItemPacket.class);
    registerOutgoing(10, ServerPlayerUseBedPacket.class);
    registerOutgoing(11, ServerAnimationPacket.class);
    registerOutgoing(12, ServerSpawnPlayerPacket.class);
    registerOutgoing(13, ServerCollectItemPacket.class);
    registerOutgoing(14, ServerSpawnObjectPacket.class);
    registerOutgoing(15, ServerSpawnMobPacket.class);
    registerOutgoing(16, ServerSpawnPaintingPacket.class);
    registerOutgoing(17, ServerSpawnExpOrbPacket.class);
    registerOutgoing(18, ServerEntityVelocityPacket.class);
    registerOutgoing(19, ServerDestroyEntitiesPacket.class);
    registerOutgoing(20, ServerEntityMovementPacket.class);
    registerOutgoing(21, ServerEntityPositionPacket.class);
    registerOutgoing(22, ServerEntityRotationPacket.class);
    registerOutgoing(23, ServerEntityPositionRotationPacket.class);
    registerOutgoing(24, ServerEntityTeleportPacket.class);
    registerOutgoing(25, ServerEntityHeadLookPacket.class);
    registerOutgoing(26, ServerEntityStatusPacket.class);
    registerOutgoing(27, ServerEntityAttachPacket.class);
    registerOutgoing(28, ServerEntityMetadataPacket.class);
    registerOutgoing(29, ServerEntityEffectPacket.class);
    registerOutgoing(30, ServerEntityRemoveEffectPacket.class);
    registerOutgoing(31, ServerSetExperiencePacket.class);
    registerOutgoing(32, ServerEntityPropertiesPacket.class);
    registerOutgoing(33, ServerChunkDataPacket.class);
    registerOutgoing(34, ServerMultiBlockChangePacket.class);
    registerOutgoing(35, ServerBlockChangePacket.class);
    registerOutgoing(36, ServerBlockValuePacket.class);
    registerOutgoing(37, ServerBlockBreakAnimPacket.class);
    registerOutgoing(38, ServerMultiChunkDataPacket.class);
    registerOutgoing(39, ServerExplosionPacket.class);
    registerOutgoing(40, ServerPlayEffectPacket.class);
    registerOutgoing(41, ServerPlaySoundPacket.class);
    registerOutgoing(42, ServerSpawnParticlePacket.class);
    registerOutgoing(43, ServerNotifyClientPacket.class);
    registerOutgoing(44, ServerSpawnGlobalEntityPacket.class);
    registerOutgoing(45, ServerOpenWindowPacket.class);
    registerOutgoing(46, ServerCloseWindowPacket.class);
    registerOutgoing(47, ServerSetSlotPacket.class);
    registerOutgoing(48, ServerWindowItemsPacket.class);
    registerOutgoing(49, ServerWindowPropertyPacket.class);
    registerOutgoing(50, ServerConfirmTransactionPacket.class);
    registerOutgoing(51, ServerUpdateSignPacket.class);
    registerOutgoing(52, ServerMapDataPacket.class);
    registerOutgoing(53, ServerUpdateTileEntityPacket.class);
    registerOutgoing(54, ServerOpenTileEntityEditorPacket.class);
    registerOutgoing(55, ServerStatisticsPacket.class);
    registerOutgoing(56, ServerPlayerListEntryPacket.class);
    registerOutgoing(57, ServerPlayerAbilitiesPacket.class);
    registerOutgoing(58, ServerTabCompletePacket.class);
    registerOutgoing(59, ServerScoreboardObjectivePacket.class);
    registerOutgoing(60, ServerUpdateScorePacket.class);
    registerOutgoing(61, ServerDisplayScoreboardPacket.class);
    registerOutgoing(62, ServerTeamPacket.class);
    registerOutgoing(63, ServerPluginMessagePacket.class);
    registerOutgoing(64, ServerDisconnectPacket.class);
    registerOutgoing(65, ServerDifficultyPacket.class);
    registerOutgoing(66, ServerCombatPacket.class);
    registerOutgoing(67, ServerSwitchCameraPacket.class);
    registerOutgoing(68, ServerWorldBorderPacket.class);
    registerOutgoing(69, ServerTitlePacket.class);
    registerOutgoing(70, ServerSetCompressionPacket.class);
    registerOutgoing(71, ServerPlayerListDataPacket.class);
    registerOutgoing(72, ServerResourcePackSendPacket.class);
    registerOutgoing(73, ServerEntityNBTUpdatePacket.class);
  }
  
  private void initClientStatus(Session session)
  {
    registerIncoming(0, StatusResponsePacket.class);
    registerIncoming(1, StatusPongPacket.class);
    
    registerOutgoing(0, StatusQueryPacket.class);
    registerOutgoing(1, StatusPingPacket.class);
  }
  
  private void initServerStatus(Session session)
  {
    registerIncoming(0, StatusQueryPacket.class);
    registerIncoming(1, StatusPingPacket.class);
    
    registerOutgoing(0, StatusResponsePacket.class);
    registerOutgoing(1, StatusPongPacket.class);
  }
}
