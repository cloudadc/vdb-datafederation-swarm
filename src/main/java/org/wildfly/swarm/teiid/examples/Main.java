package org.wildfly.swarm.teiid.examples;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.wildfly.swarm.Swarm;
import org.wildfly.swarm.datasources.DatasourcesFraction;
import org.wildfly.swarm.resource.adapters.ResourceAdapterFraction;
import org.wildfly.swarm.teiid.TeiidFraction;
import org.wildfly.swarm.teiid.VDBArchive;

public class Main {
    
    private static String URL = "jdbc:mysql://" + System.getProperty("MYSQL_HOST", "mysql") + "/" + System.getProperty("MYSQL_DB", "sample");
    private static String marketdataDir;
    
    
    public static void main(String[] args) throws Exception {
        
        System.out.println("###########################");
        System.out.println("MYSQL_HOST: " + System.getProperty("MYSQL_HOST", "mysql"));
        System.out.println("MYSQL_DB: " + System.getProperty("MYSQL_DB", "sample"));
        System.out.println("MYSQL_USER: " + System.getProperty("MYSQL_USER", "test_user"));
        System.out.println("MYSQL_PASSWORD: " + System.getProperty("MYSQL_PASSWORD", "test_pass"));
        System.out.println("###########################");
        
        setupSampleSources();
        
        Swarm swarm = new Swarm();        
        swarm.fraction(new TeiidFraction()
                 .translator("mysql", t -> t.module("org.jboss.teiid.translator.jdbc"))
                 .translator("file", t -> t.module("org.jboss.teiid.translator.file")))
             .fraction(new DatasourcesFraction()
                 .dataSource("accounts-ds", ds -> {
                     ds.driverName("mysql"); // the 'h2' are auto-detected, and registered dynamic 
                     ds.connectionUrl(URL);
                     ds.userName(System.getProperty("MYSQL_USER", "test_user"));
                     ds.password(System.getProperty("MYSQL_PASSWORD", "test_pass"));
                 }))
             .fraction(new ResourceAdapterFraction()
                 .resourceAdapter("fileQS", rac -> rac.module("org.jboss.teiid.resource-adapter.file")
                     .connectionDefinitions("fileQS", cdc -> cdc.className("org.teiid.resource.adapter.file.FileManagedConnectionFactory")
                         .jndiName("java:/marketdata-file")
                         .configProperties("ParentDirectory", cpc -> cpc.value(marketdataDir))
                         .configProperties("AllowParentPaths", cpc -> cpc.value("true")))));
        swarm.start();

        VDBArchive vdb = ShrinkWrap.create(VDBArchive.class);
        vdb.vdb(Main.class.getClassLoader().getResourceAsStream("portfolio-vdb.xml"));
        swarm.deploy(vdb);   
    }

    private static void setupSampleSources() throws Exception {

        
        Path target = Paths.get(System.getProperty("java.io.tmpdir"), "teiidfiles");
        marketdataDir = target.toString();
        if(!Files.exists(target)){
            Files.createDirectories(target);
        }
        target.toFile().deleteOnExit();
        Files.copy(Main.class.getClassLoader().getResourceAsStream("teiidfiles/data/marketdata-price.txt"), target.resolve("marketdata-price.txt"), REPLACE_EXISTING);
        Files.copy(Main.class.getClassLoader().getResourceAsStream("teiidfiles/data/marketdata-price1.txt"), target.resolve("marketdata-price1.txt"), REPLACE_EXISTING);
  
    }
    
}
