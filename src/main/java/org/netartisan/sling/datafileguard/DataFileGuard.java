package org.netartisan.sling.datafileguard;

import java.util.Iterator;

import javax.jcr.Session;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import javax.jcr.version.VersionManager;
import javax.jcr.version.Version;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.settings.SlingSettingsService;
import org.apache.jackrabbit.oak.plugins.blob.datastore.OakFileDataStore;
import org.apache.jackrabbit.oak.plugins.blob.datastore.FileDataStoreService;
import org.apache.jackrabbit.core.data.DataRecord;
import org.apache.jackrabbit.core.data.DataIdentifier;
import org.apache.jackrabbit.core.data.FileDataStore;
import org.apache.jackrabbit.oak.plugins.blob.datastore.DataStoreBlobStore;
import org.apache.jackrabbit.oak.spi.blob.BlobStore;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component( 
	service = DataFileGuardService.class, 
	immediate = true, 
	configurationPolicy = ConfigurationPolicy.OPTIONAL, 
	property = {
		Constants.SERVICE_DESCRIPTION
				+ "=org.netartisan.sling.gcc4ame - GCC4AEM JavaScript Processor",
		Constants.SERVICE_RANKING + ":Integer="+Integer.MAX_VALUE
	}
)
@Designate(ocd = DataFileGuard.Config.class, factory=true)

public class DataFileGuard implements DataFileGuardService {

	@ObjectClassDefinition(
			name = "org.netartisan.sling.gcc4aem - DataFile Guard",
			description = "DataFile Gard Sling Jackrabbit Oak Data File Store"
	)
	public @interface Config {
		@AttributeDefinition(
				name = "Service Ranking",
				description = "Higher value gives higher execution priority.",
				min = "1",
				max = Integer.MAX_VALUE+"",
				required = true, // Defaults to true
				cardinality = 0
		)
		int service_ranking() default Integer.MAX_VALUE;
	}

	protected Logger log = LoggerFactory.getLogger(this.getClass());
	protected ComponentContext componentContext = null;
	protected BundleContext bundleContext;
	protected Config bundleConfig;

	protected SlingRepository repository = null;
	protected ConfigurationAdmin ca = null; 
	protected SlingSettingsService settings = null;	
	
	@Activate
	protected void activate(ComponentContext ctx, Config cfg) {
		log.info("activate");
		modified( ctx, cfg );
		try {
			listBlobs();
		} catch (Exception e ) {
			log.error( e.getMessage() );
		}
	}
	
	@Modified
	protected void modified(ComponentContext ctx, Config cfg) {
		log.info("modified");
		this.componentContext = ctx;
		this.bundleContext = ctx.getBundleContext();
		this.bundleConfig = cfg;
		this.repository = (SlingRepository)bundleContext.getService(bundleContext.getServiceReference( SlingRepository.class.getName() ));
		this.ca = (ConfigurationAdmin)bundleContext.getService(bundleContext.getServiceReference( ConfigurationAdmin.class.getName() ));
		this.settings = (SlingSettingsService)bundleContext.getService(bundleContext.getServiceReference( SlingSettingsService.class.getName() ));
		log.info( "Here "+this.repository + " " + this.ca +" "+ this.settings);
	}

	@Deactivate
	protected void deactivate(ComponentContext ctx, Config cfg) {
		log.info("deactivate");
		this.bundleContext = null;
		this.bundleConfig = null;
	}

	long nodeCounter = 0;
	long binaryPropCounter = 0;
	protected void visitProperties(Node n, VersionManager v) throws RepositoryException {
		for( PropertyIterator pi = n.getProperties(); pi.hasNext(); ) {
			Property p = pi.nextProperty();
			if( p.getType() == PropertyType.BINARY ) {
				/**
				VersionHistory vh = v.getVersionHistory(n.getPath());
				for( VersionIterator vi = vh.getAllLinearVersions(); vi.hasNext(); ) {
					Version v1 = vi.nextVersion();
					log.info("       *** {}", v1.getIdentifier() );
				}
				*/
				
				log.info("**** #{}: {}", binaryPropCounter++, p.getPath() );
			}
		}
	}

	protected void visitNode(Node n, VersionManager v) {
		nodeCounter = 0;
		binaryPropCounter = 0;
		visitNode(n, nodeCounter, v);
	}

	protected void visitNode(Node n, long cnt, VersionManager v) {
		try {
			log.info( "#{}: {}", cnt, n.getPath() );
			visitProperties(n, v);
			for( NodeIterator ni = n.getNodes(); ni.hasNext(); ) {
				Node cn = ni.nextNode();
				visitNode(cn, nodeCounter++, v);
			}
		} catch (RepositoryException e) {
			// TODO Auto-generated catch block
			log.error(e.getMessage());
		}
	}
	
	@Override
	public void listBlobs() throws Exception {
		// TODO Auto-generated method stub
		log.info("list blobs");
		@SuppressWarnings("deprecation")
		Session sess = repository.loginAdministrative(null);
		VersionManager vm = sess.getWorkspace().getVersionManager();
		Node n = sess.getRootNode();
		nodeCounter = 0;
		visitNode(n, vm);
		/*
		BlobStore bs = (BlobStore)bundleContext.getService(bundleContext.getServiceReference( BlobStore.class.getName() ));
		DataStoreBlobStore fds = (DataStoreBlobStore)bs;
		int cnt = 0; 
		for( Iterator<DataRecord> idr = fds.getAllRecords(); idr.hasNext(); ) {
			DataRecord dr = idr.next();
			DataIdentifier di = dr.getIdentifier();
			// log.info("#{}({}) - {} ? {}", ++cnt, bs.getBlobLength(bs.getBlobId(dr.getReference())), dr.getReference(), di.toString() );
			log.info("#{} -- ({})", ++cnt, fds.resolveChunks(fds.getBlobId(dr.getReference())) );
			for( Iterator<String> istr = fds.resolveChunks(fds.getBlobId(dr.getReference())); istr.hasNext(); ) {
				String str = istr.next();
				log.info("   #{} -- {}", cnt, str);
			}
		}
		*/
		sess.logout();
	}
}
