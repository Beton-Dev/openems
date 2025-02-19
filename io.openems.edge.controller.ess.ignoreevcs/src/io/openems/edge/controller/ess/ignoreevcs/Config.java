package io.openems.edge.controller.ess.ignoreevcs;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller Ess: optimized for evcs", //
		description = "Checks meter and subtracts evcs-load from household load, ess will never be discharged for the evcs")
@interface Config {

    @AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
    String id() default "ctrlEssEvcs0";

    @AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

    @AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;
    
    @AttributeDefinition(name = "Ess-ID", description = "ID of Ess device.")
	String ess_id();
    
    @AttributeDefinition(name = "Evcs-ID", description = "ID of Evcs device.")
	String evcs_id();
    
    @AttributeDefinition(name = "Batteryinverter-ID", description = "ID of Batteryinverter device")
	String batteryinverter_id();
    
    @AttributeDefinition(name = "Meter-ID", description = "ID of Batteryinverter device")
	String meter_id();

}
