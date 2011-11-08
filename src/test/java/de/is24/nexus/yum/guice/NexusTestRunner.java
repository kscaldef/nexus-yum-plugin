package de.is24.nexus.yum.guice;

import static de.is24.nexus.yum.repository.utils.RepositoryTestUtils.BASE_CACHE_DIR;
import static org.apache.commons.io.FileUtils.copyDirectory;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.codehaus.plexus.logging.Logger.LEVEL_INFO;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import java.io.File;
import java.io.IOException;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import de.is24.nexus.yum.plugin.RepositoryRegistry;
import de.is24.nexus.yum.plugin.impl.DefaultRepositoryRegistry;
import de.is24.nexus.yum.service.AliasMapper;
import de.is24.nexus.yum.service.RepositoryAliasService;
import de.is24.nexus.yum.service.RepositoryCreationTimeoutHolder;
import de.is24.nexus.yum.service.RepositoryRpmManager;
import de.is24.nexus.yum.service.YumService;
import de.is24.nexus.yum.service.impl.DefaultRepositoryAliasService;
import de.is24.nexus.yum.service.impl.DefaultRepositoryRpmManager;
import de.is24.nexus.yum.service.impl.DefaultYumService;
import de.is24.nexus.yum.service.impl.ThreadPoolYumRepositoryCreatorService;
import de.is24.nexus.yum.service.impl.YumConfigurationHandler;
import de.is24.nexus.yum.service.impl.YumRepositoryCreatorService;
import de.is24.test.guice.GuiceTestRunner;
import org.codehaus.plexus.logging.slf4j.Slf4jLogger;
import org.junit.runners.model.InitializationError;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.configuration.application.GlobalRestApiSettings;
import org.sonatype.nexus.configuration.application.NexusConfiguration;
import org.sonatype.plexus.appevents.ApplicationEventMulticaster;
import org.sonatype.plexus.appevents.SimpleApplicationEventMulticaster;


public class NexusTestRunner extends GuiceTestRunner {
  public NexusTestRunner(final Class<?> classToRun) throws InitializationError {
    super(classToRun, new AbstractModule() {
        @Override
        protected void configure() {
          bind(NexusConfiguration.class).toInstance(createNexusConfiguration());
          bind(GlobalRestApiSettings.class).toInstance(createRestApiSettings());
          bind(YumRepositoryCreatorService.class).to(ThreadPoolYumRepositoryCreatorService.class);
          bind(YumService.class).annotatedWith(Names.named(YumService.DEFAULT_BEAN_NAME)).to(DefaultYumService.class);
          bind(ApplicationEventMulticaster.class).toInstance(createEventMulticaster());
          bind(RepositoryRegistry.class).annotatedWith(Names.named(RepositoryRegistry.DEFAULT_BEAN_NAME)).to(
            DefaultRepositoryRegistry.class);
          bind(RepositoryRpmManager.class).annotatedWith(Names.named(RepositoryRpmManager.DEFAULT_BEAN_NAME)).to(
            DefaultRepositoryRpmManager.class);

          bind(RepositoryAliasService.class).annotatedWith(Names.named(RepositoryAliasService.DEFAULT_BEAN_NAME)).to(
            DefaultRepositoryAliasService.class);
          bind(AliasMapper.class).annotatedWith(Names.named(RepositoryCreationTimeoutHolder.DEFAULT_BEAN_NAME)).to(
            YumConfigurationHandler.class);
          bind(AliasMapper.class).to(YumConfigurationHandler.class);

          bind(RepositoryCreationTimeoutHolder.class).to(YumConfigurationHandler.class);
          bind(RepositoryCreationTimeoutHolder.class).annotatedWith(
            Names.named(RepositoryCreationTimeoutHolder.DEFAULT_BEAN_NAME)).to(
            YumConfigurationHandler.class);
        }

        private ApplicationEventMulticaster createEventMulticaster() {
          SimpleApplicationEventMulticaster eventMulticaster = new SimpleApplicationEventMulticaster();
          eventMulticaster.enableLogging(
            new Slf4jLogger(LEVEL_INFO, LoggerFactory.getLogger(SimpleApplicationEventMulticaster.class)));
          return eventMulticaster;
        }

        private NexusConfiguration createNexusConfiguration() {
          NexusConfiguration config = createMock(NexusConfiguration.class);
          expect(config.getTemporaryDirectory()).andReturn(BASE_CACHE_DIR).anyTimes();

          File tmpDir = cloneToTempDir(new File(".", "target/test-classes/nexus/sonatype-work/nexus/conf/"));
          expect(config.getConfigurationDirectory()).andReturn(tmpDir).anyTimes();
          replay(config);
          return config;
        }

        private File cloneToTempDir(File sourceDir) {
          File tmpDir = new File(".", "target/tmp/" + randomAlphabetic(10));
          tmpDir.mkdirs();
          try {
            copyDirectory(sourceDir, tmpDir);
          } catch (IOException e) {
            throw new RuntimeException("Could not copy nexus configuration to a temp dir : " + tmpDir, e);
          }
          return tmpDir;
        }

        private GlobalRestApiSettings createRestApiSettings() {
          GlobalRestApiSettings settings = createMock(GlobalRestApiSettings.class);
          expect(settings.getBaseUrl()).andReturn("http://localhost:8081/nexus").anyTimes();
          replay(settings);
          return settings;
        }
      });
  }
}