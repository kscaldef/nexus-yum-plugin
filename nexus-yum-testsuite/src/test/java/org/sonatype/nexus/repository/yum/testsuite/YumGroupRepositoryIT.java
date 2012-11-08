/**
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2007-2012 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.repository.yum.testsuite;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.sonatype.nexus.repository.yum.client.MetadataType.PRIMARY_XML;

import java.net.URISyntaxException;

import org.junit.Rule;
import org.junit.Test;
import org.sonatype.nexus.client.core.subsystem.artifact.UploadRequest;
import org.sonatype.nexus.client.core.subsystem.repository.GroupRepository;
import org.sonatype.nexus.client.core.subsystem.repository.maven.MavenHostedRepository;
import org.sonatype.nexus.repository.yum.client.YumGroupRepository;
import org.sonatype.nexus.test.os.OsTestRule;

public class YumGroupRepositoryIT
    extends YumRepositoryITSupport
{

    @Rule
    public OsTestRule osTestRule = new OsTestRule();

    public YumGroupRepositoryIT( final String nexusBundleCoordinates )
    {
        super( nexusBundleCoordinates );
    }

    @Test
    public void shouldRegenerateRepoAfterUpload()
        throws Exception
    {
        final GroupRepository groupRepo = givenGroupRepoWith2RPMs();

        final String primaryXml = getPrimaryXmlOf( groupRepo );
        assertThat( primaryXml, containsString( "test-artifact" ) );
        assertThat( primaryXml, containsString( "test-rpm" ) );
    }

    @Test
    public void shouldRegenerateGroupRepoWhenMemberRepoIsRemoved()
        throws Exception
    {
        final GroupRepository groupRepo = givenGroupRepoWith2RPMs();
        groupRepo.removeMember( groupRepo.memberRepositories().get( 0 ).toString() ).save();

        sleep( 5, SECONDS );

        final String primaryXml = getPrimaryXmlOf( groupRepo );
        assertThat( primaryXml, containsString( "test-artifact" ) );
        assertThat( primaryXml, not( containsString( "test-rpm" ) ) );
    }

    @Test
    public void shouldRegenerateGroupRepoWhenMemberRepoIsAdded()
        throws Exception
    {
        final GroupRepository groupRepo = givenGroupRepoWith2RPMs();

        final MavenHostedRepository repo3 = repositories().create(
            MavenHostedRepository.class, repositoryIdForTest( "3" )
        ).save();

        mavenArtifact().upload(
            new UploadRequest(
                repo3.id(), "a_group3", "an_artifact3", "3.0", "pom", "", "rpm",
                testData().resolveFile( "/rpms/foo-bar-5.1.2-1.noarch.rpm" )
            )
        );

        sleep( 5, SECONDS );

        groupRepo.addMember( repo3.id() ).save();

        sleep( 5, SECONDS );

        final String primaryXml = getPrimaryXmlOf( groupRepo );

        assertThat( primaryXml, containsString( "test-artifact" ) );
        assertThat( primaryXml, containsString( "test-rpm" ) );
        assertThat( primaryXml, containsString( "foo-bar" ) );
    }

    private String getPrimaryXmlOf( final GroupRepository groupRepo )
    {
        return yum().getMetadata( groupRepo.id(), PRIMARY_XML, String.class );
    }

    private YumGroupRepository givenGroupRepoWith2RPMs()
        throws InterruptedException, URISyntaxException
    {
        final MavenHostedRepository repo1 = repositories().create(
            MavenHostedRepository.class, repositoryIdForTest( "1" )
        ).save();

        final MavenHostedRepository repo2 = repositories().create(
            MavenHostedRepository.class, repositoryIdForTest( "2" )
        ).save();

        final YumGroupRepository groupRepo = repositories().create( YumGroupRepository.class, repositoryIdForTest() )
            .ofRepositories( repo1.id(), repo2.id() )
            .save();

        sleep( 5, SECONDS );

        mavenArtifact().upload(
            new UploadRequest(
                repo1.id(), "a_group1", "an_artifact1", "1.0", "pom", "", "rpm",
                testData().resolveFile( "/rpms/test-artifact-1.2.3-1.noarch.rpm" )
            )
        );
        mavenArtifact().upload(
            new UploadRequest(
                repo2.id(), "a_group2", "an_artifact2", "2.0", "pom", "", "rpm",
                testData.resolveFile( "/rpms/test-rpm-5.6.7-1.noarch.rpm" )
            )
        );

        sleep( 5, SECONDS );

        return groupRepo;
    }

}