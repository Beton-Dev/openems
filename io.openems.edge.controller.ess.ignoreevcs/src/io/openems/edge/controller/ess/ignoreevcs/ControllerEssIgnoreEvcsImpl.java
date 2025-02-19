package io.openems.edge.controller.ess.ignoreevcs;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.dccharger.api.EssDcCharger;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.goodwe.batteryinverter.GoodWeBatteryInverter;
import io.openems.edge.meter.api.ElectricityMeter;

@Designate(ocd = Config.class, factory = true)
@Component(
    name = "ess.optimizedevcs",
    immediate = true,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class ControllerEssIgnoreEvcsImpl extends AbstractOpenemsComponent implements ControllerEssIgnoreEvcs, Controller, OpenemsComponent {

    private Config config = null;
    
    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private ElectricityMeter meter;

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private ManagedSymmetricEss ess;
    
    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private ManagedEvcs evcs;
    
    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private EssDcCharger charger;

    private static final int HYSTERESE = 100; // Mindeständerung in Watt
    private static final int MAX_CHANGE_RATE = 500; // Maximal 500 W Änderung pro Zyklus

    private double Kp = 0.3;  // Proportionalfaktor
    private double Ki = 0.01; // Integrationsfaktor
    private double Kd = 0.05; // Differenzialfaktor
    private double lastError = 0;
    private double integral = 0;
    private int lastSetPower = 0; // Letzte Batterieleistung

    public ControllerEssIgnoreEvcsImpl() {
        super(OpenemsComponent.ChannelId.values(), Controller.ChannelId.values(), ControllerEssIgnoreEvcs.ChannelId.values());
    }

    @Activate
    private void activate(ComponentContext context, Config config) {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this.config = config;
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    public void run() throws OpenemsNamedException {
        
    		// Get Data From Meter/EVCS/Charger
            int loadPower = meter.getActivePower().get();
            int evcsPower = evcs.getActivePower().get();
            int pvPower = charger.getActualPower().get();
            
            int targetPower = pvPower - (loadPower - evcsPower);

            double error = targetPower - lastSetPower;
            integral += error;
            double derivative = error - lastError;
            double pidOutput = (Kp * error) + (Ki * integral) + (Kd * derivative);
            lastError = error;

            int newSetPower = lastSetPower + (int) pidOutput;
            if (Math.abs(newSetPower - lastSetPower) > MAX_CHANGE_RATE) {
                newSetPower = lastSetPower + (newSetPower > lastSetPower ? MAX_CHANGE_RATE : -MAX_CHANGE_RATE);
            }

            if (Math.abs(newSetPower - lastSetPower) > HYSTERESE) {
                ess.setActivePowerEqualsWithPid(newSetPower);
                lastSetPower = newSetPower;
            }
    }
}
