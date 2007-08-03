package org.gudy.azureus2.ui.swt.views.configsections;

import org.gudy.azureus2.ui.swt.plugins.UISWTConfigSection;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.core3.util.AEDiagnostics;
import org.gudy.azureus2.core3.util.AEDiagnosticsLogger;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.impl.TransferSpeedValidator;
import org.gudy.azureus2.core3.config.impl.ConfigurationDefaults;
import org.gudy.azureus2.core3.config.impl.ConfigurationParameterNotFoundException;

import org.eclipse.swt.widgets.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.SWT;
import com.aelitis.azureus.core.speedmanager.impl.SpeedManagerImpl;
import com.aelitis.azureus.core.speedmanager.impl.v2.SpeedLimitMonitor;
import com.aelitis.azureus.core.speedmanager.impl.v2.SpeedManagerAlgorithmProviderV2;

/**
 * Created on May 15, 2007
 * Created by Alan Snyder
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
 * <p/>
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * <p/>
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */

public class ConfigSectionTransferAutoSpeedBeta
        implements UISWTConfigSection
{

    //upload/download limits
    IntParameter downMaxLim;
    IntParameter uploadMaxLim;

    StringListParameter confDownload;
    StringListParameter confUpload;

    //add a comment to the auto-speed debug logs.
    Group commentGroup;

    Group uploadCapGroup;

    //DHT ping set-points
    Group dhtGroup;
    IntParameter dGood;
    IntParameter dGoodTol;
    IntParameter dBad;
    IntParameter dBadTol;
    //general ping set-points.
    IntParameter adjustmentInterval;
    BooleanParameter skipAfterAdjustment;

    Button reset;

    IntListParameter downloadModeUsedCap;
    IntListParameter seedModeUsedCap;

    /**
     * Create your own configuration panel here.  It can be anything that inherits
     * from SWT's Composite class.
     * Please be mindfull of small screen resolutions
     *
     * @param parent The parent of your configuration panel
     * @return your configuration panel
     */

    /**
     * Returns section you want your configuration panel to be under.
     * See SECTION_* constants.  To add a subsection to your own ConfigSection,
     * return the configSectionGetName result of your parent.<br>
     */
    public String configSectionGetParentSection() {
        return "transfer.select";
    }

    /**
     * In order for the plugin to display its section correctly, a key in the
     * Plugin language file will need to contain
     * <TT>ConfigView.section.<i>&lt;configSectionGetName() result&gt;</i>=The Section name.</TT><br>
     *
     * @return The name of the configuration section
     */
    public String configSectionGetName() {
        return "transfer.select.v2";
    }

    /**
     * User selected Save.
     * All saving of non-plugin tabs have been completed, as well as
     * saving of plugins that implement org.gudy.azureus2.plugins.ui.config
     * parameters.
     */
    public void configSectionSave() {
        
    }

    /**
     * Config view is closing
     */
    public void configSectionDelete() {

    }


    public Composite configSectionCreate(final Composite parent) {

        GridData gridData;

        Composite cSection = new Composite(parent, SWT.NULL);

        gridData = new GridData(GridData.VERTICAL_ALIGN_FILL|GridData.HORIZONTAL_ALIGN_FILL);
        cSection.setLayoutData(gridData);
        GridLayout subPanel = new GridLayout();
        subPanel.numColumns = 3;
        cSection.setLayout(subPanel);

        //add a comment to the debug log.
        ///////////////////////////////////
        // Comment group
        ///////////////////////////////////
        //comment grouping.
        commentGroup = new Group(cSection, SWT.NULL);
        Messages.setLanguageText(commentGroup,"ConfigTransferAutoSpeed.add.comment.to.log.group");
        GridLayout commentLayout = new GridLayout();
        commentLayout.numColumns = 3;
        commentGroup.setLayout(commentLayout);
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        commentGroup.setLayoutData(gridData);

        //Label
        Label commentLabel = new Label(commentGroup,SWT.NULL);
        Messages.setLanguageText(commentLabel,"ConfigTransferAutoSpeed.add.comment.to.log");
        gridData = new GridData();
        gridData.widthHint = 70;
        gridData.horizontalSpan=1;
        commentLabel.setLayoutData(gridData);

        //Text-Box
        final Text commentBox = new Text(commentGroup, SWT.BORDER);
        gridData = new GridData(GridData.BEGINNING);
        gridData.widthHint = 200;
        gridData.horizontalSpan=1;
        commentBox.setText("");
        commentBox.setLayoutData(gridData);


        //button
        Button commentButton = new Button( commentGroup, SWT.PUSH);
        //Messages.
        gridData = new GridData();
        gridData.widthHint = 70;
        gridData.horizontalSpan=1;
        commentButton.setLayoutData(gridData);
        Messages.setLanguageText(commentButton,"ConfigTransferAutoSpeed.log.button");
        commentButton.addListener(SWT.Selection, new Listener(){
            public void handleEvent(Event event){
                //Add a file to the log.
                AEDiagnosticsLogger dLog = AEDiagnostics.getLogger("AutoSpeed");
                String comment = commentBox.getText();
                if(comment!=null){
                    if( comment.length()>0){
                        dLog.log( "user-comment:"+comment );
                        commentBox.setText("");
                    }
                }
            }
        });

        //spacer
        Label commentSpacer = new Label(cSection, SWT.NULL);
        gridData = new GridData();
        gridData.horizontalSpan=3;
        commentSpacer.setLayoutData(gridData);

                
        ///////////////////////////////////
        // AutoSpeed Beta mode group
        ///////////////////////////////////
//        //Beta-mode grouping.
//        Group modeGroup = new Group(cSection, SWT.NULL);
//        //Messages.setLanguageText
//        modeGroup.setText("AutoSpeed-Beta settings");
//        GridLayout modeLayout = new GridLayout();
//        modeLayout.numColumns = 3;
//        modeGroup.setLayout(modeLayout);
//        gridData = new GridData(GridData.FILL_HORIZONTAL);
//        modeGroup.setLayoutData(gridData);
//
//        //spacer
//        Label cSpacer = new Label(modeGroup, SWT.NULL);
//        gridData = new GridData();
//        gridData.horizontalSpan=4;
//        cSpacer.setLayoutData(gridData);
//
//        //label column for speed test results
//        Label limits = new Label(modeGroup, SWT.NULL);
//        gridData = new GridData();
//        gridData.widthHint=80;
//        limits.setText("Line Speed Limits: ");
//        //Messages.setLanguageText
//
//        Label limMax = new Label(modeGroup,SWT.NULL);
//        gridData = new GridData();
//        limMax.setLayoutData(gridData);
//        limMax.setText("max - capacity");
//        //Messages.setLanguageText
//
//        Label confLevel = new Label(modeGroup, SWT.NULL);
//        gridData =  new GridData();
//        confLevel.setLayoutData(gridData);
//        confLevel.setText("confidence level");
//        //Messages.setLanguageText
//
//        //download settings
//        Label setDown = new Label(modeGroup, SWT.NULL);
//        gridData = new GridData();
//        setDown.setLayoutData(gridData);
//        setDown.setText("Download: ");
//        //Messages.setLanguageText
//
//        gridData = new GridData();
//        gridData.widthHint = 80;
//        downMaxLim = new IntParameter(modeGroup,SpeedManagerAlgorithmProviderV2.SETTING_DOWNLOAD_MAX_LIMIT);
//        downMaxLim.setLayoutData( gridData );
//
//        String[] confLevelNames = {
//                SpeedLimitConfidence.ABSOLUTE.getInternationalizedString(),
//                SpeedLimitConfidence.HIGH.getInternationalizedString(),
//                SpeedLimitConfidence.MED.getInternationalizedString(),
//                SpeedLimitConfidence.LOW.getInternationalizedString(),
//                SpeedLimitConfidence.NONE.getInternationalizedString()
//        };
//
//        String[] confLevelValues = {
//                SpeedLimitConfidence.ABSOLUTE.getString(),
//                SpeedLimitConfidence.HIGH.getString(),
//                SpeedLimitConfidence.MED.getString(),
//                SpeedLimitConfidence.LOW.getString(),
//                SpeedLimitConfidence.NONE.getString()
//        };
//
//        gridData = new GridData();
//        gridData.widthHint = 80;
//        confDownload = new StringListParameter(modeGroup, SpeedLimitMonitor.DOWNLOAD_CONF_LIMIT_SETTING, confLevelNames, confLevelValues);
//        confDownload.setLayoutData( gridData );
//
//
//        //upload settings
//        Label setUp = new Label(modeGroup, SWT.NULL);
//        gridData = new GridData();
//        setUp.setLayoutData(gridData);
//        setUp.setText("Upload: ");
//        //Messages.setLanguageText
//
//        gridData = new GridData();
//        gridData.widthHint = 80;
//        uploadMaxLim = new IntParameter(modeGroup, SpeedManagerAlgorithmProviderV2.SETTING_UPLOAD_MAX_LIMIT);
//        uploadMaxLim.setLayoutData( gridData );
//
//        gridData = new GridData();
//        gridData.widthHint = 80;
//        confUpload = new StringListParameter(modeGroup, SpeedLimitMonitor.UPLOAD_CONF_LIMIT_SETTING,confLevelNames,confLevelValues);
//        confUpload.setLayoutData( gridData );
//
//        //spacer
//        cSpacer = new Label(modeGroup, SWT.NULL);
//        gridData = new GridData();
//        gridData.horizontalSpan=4;
//        cSpacer.setLayoutData(gridData);
//
//        //Restore Defaults:
//        Label restorDef = new Label(modeGroup, SWT.NULL);
//        gridData = new GridData();
//        restorDef.setLayoutData(gridData);
//        restorDef.setText("Restore Defaults:");
//
//        //Button and listener here.
//        reset = new Button(modeGroup, SWT.PUSH);
//        reset.setText("Reset");
//        gridData = new GridData();
//        gridData.widthHint = 70;
//        reset.setLayoutData(gridData);
//        reset.addListener(SWT.Selection, new RestoreDefaultsListener());
//        //
//
//        //spacer
//        cSpacer = new Label(cSection, SWT.NULL);
//        gridData = new GridData();
//        gridData.horizontalSpan=4;
//        cSpacer.setLayoutData(gridData);
//
        ///////////////////////////
        // Upload Capacity used settings.
        ///////////////////////////
        uploadCapGroup = new Group(cSection, SWT.NULL);
        //uploadCapGroup.setText("Upload Capacity Usage");//ToDo: internationalize.
        Messages.setLanguageText(uploadCapGroup,"ConfigTransferAutoSpeed.upload.capacity.usage");

        GridLayout uCapLayout = new GridLayout();
        uCapLayout.numColumns = 2;
        uploadCapGroup.setLayout(uCapLayout);

        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 3;
        uploadCapGroup.setLayoutData(gridData);

        //Label column
        Label upCapModeLbl = new Label(uploadCapGroup, SWT.NULL);
        gridData = new GridData();
        gridData.widthHint = 80;
        upCapModeLbl.setLayoutData(gridData);
        //upCapModeLbl.setText("Mode:");//ToDo: internationalize.
        Messages.setLanguageText(upCapModeLbl,"ConfigTransferAutoSpeed.mode");


        Label ucSetLbl = new Label(uploadCapGroup, SWT.NULL);
        gridData = new GridData();
        gridData.widthHint = 80;
        gridData.horizontalSpan = 2;
        //ucSetLbl.setText("% Capacity Used");//ToDo: internationalize.
        Messages.setLanguageText(ucSetLbl,"ConfigTransferAutoSpeed.capacity.used");

        Label dlModeLbl = new Label(uploadCapGroup, SWT.NULL);
        gridData = new GridData();
        gridData.widthHint = 80;
        //dlModeLbl.setText("Downloading:");//ToDo: internationalize.
        Messages.setLanguageText(dlModeLbl,"ConfigTransferAutoSpeed.while.downloading");

        //add a drop down.
        String[] downloadModeNames = {
                " 80%",
                " 70%",
                " 60%",
                " 50%"
        };

        int[] downloadModeValues = {
                80,
                70,
                60,
                50
        };

        downloadModeUsedCap = new IntListParameter(uploadCapGroup,
                SpeedLimitMonitor.USED_UPLOAD_CAPACITY_DOWNLOAD_MODE,
                downloadModeNames, downloadModeValues);


        //spacer
        Label cSpacer = new Label(cSection, SWT.NULL);
        gridData = new GridData();
        gridData.horizontalSpan=4;
        cSpacer.setLayoutData(gridData);

        //////////////////////////
        // DHT Ping Group
        //////////////////////////

        dhtGroup = new Group(cSection, SWT.NULL);
        //Messages.setLanguageText
        dhtGroup.setText("Data: DHT Pings");
        dhtGroup.setLayout(subPanel);

        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 3;
        dhtGroup.setLayoutData(gridData);

        //label column for Vivaldi limits
        Label dhtSetting = new Label(dhtGroup, SWT.NULL);
        gridData = new GridData();
        gridData.widthHint=80;
        //dhtSetting.setText("DHT Ping Settings: ");
        Messages.setLanguageText(dhtSetting,"ConfigTransferAutoSpeed.set.dht.ping"); //ToDo: internationalize

        Label dSet = new Label(dhtGroup,SWT.NULL);
        gridData = new GridData();
        dSet.setLayoutData(gridData);
        //dSet.setText("set point (ms)");
        Messages.setLanguageText(dSet,"ConfigTransferAutoSpeed.set.point"); //ToDo: internationalize

        Label dTol = new Label(dhtGroup, SWT.NULL);
        gridData = new GridData();
        dTol.setLayoutData(gridData);
        //dTol.setText("tolerance (ms)");
        Messages.setLanguageText(dTol,"ConfigTransferAutoSpeed.set.tolerance"); //ToDo: internationalize

        //good
        Label dGoodLbl = new Label(dhtGroup, SWT.NULL);
        gridData = new GridData();
        dGoodLbl.setLayoutData(gridData);
        //dGoodLbl.setText("Good: ");
        Messages.setLanguageText(dGoodLbl,"ConfigTransferAutoSpeed.ping.time.good"); //ToDo: internationalize


        gridData = new GridData();
        gridData.widthHint = 50;
        dGood = new IntParameter(dhtGroup, SpeedManagerAlgorithmProviderV2.SETTING_DHT_GOOD_SET_POINT);
        dGood.setLayoutData( gridData );

        gridData = new GridData();
        gridData.widthHint = 50;
        dGoodTol = new IntParameter(dhtGroup, SpeedManagerAlgorithmProviderV2.SETTING_DHT_GOOD_TOLERANCE);
        dGoodTol.setLayoutData( gridData );

        //bad
        Label dBadLbl = new Label(dhtGroup, SWT.NULL);
        gridData = new GridData();
        dBadLbl.setLayoutData(gridData);
        //dBadLbl.setText("Bad: ");
        Messages.setLanguageText(dBadLbl,"ConfigTransferAutoSpeed.ping.time.bad"); //ToDo: internationalize


        gridData = new GridData();
        gridData.widthHint = 50;
        dBad = new IntParameter(dhtGroup, SpeedManagerAlgorithmProviderV2.SETTING_DHT_BAD_SET_POINT);
        dBad.setLayoutData( gridData );


        gridData = new GridData();
        gridData.widthHint = 50;
        dBadTol = new IntParameter(dhtGroup, SpeedManagerAlgorithmProviderV2.SETTING_DHT_BAD_TOLERANCE);
        dBadTol.setLayoutData( gridData );

        //spacer
        cSpacer = new Label(cSection, SWT.NULL);
        gridData = new GridData();
        gridData.horizontalSpan=1;
        cSpacer.setLayoutData(gridData);

        //how much data to accumulate before making an adjustment.
        Label iCount = new Label(dhtGroup, SWT.NULL);
        gridData = new GridData();
        gridData.horizontalSpan=2;
        gridData.horizontalAlignment=GridData.BEGINNING;
        iCount.setLayoutData(gridData);
        //iCount.setText("Adjustment interval: ");
        Messages.setLanguageText(iCount,"ConfigTransferAutoSpeed.adjustment.interval");

        adjustmentInterval = new IntParameter(dhtGroup, SpeedManagerAlgorithmProviderV2.SETTING_INTERVALS_BETWEEN_ADJUST);
        gridData = new GridData();
        gridData.widthHint = 50;
        adjustmentInterval.setLayoutData(gridData);

        //spacer
        cSpacer = new Label(cSection, SWT.NULL);
        gridData = new GridData();
        gridData.horizontalSpan=1;
        cSpacer.setLayoutData(gridData);

        //how much data to accumulate before making an adjustment.
        Label skip = new Label(dhtGroup, SWT.NULL);
        gridData = new GridData();
        gridData.horizontalSpan=2;
        gridData.horizontalAlignment=GridData.BEGINNING;
        skip.setLayoutData(gridData);
        //skip.setText("Skip after adjustment: ");
        Messages.setLanguageText(skip,"ConfigTransferAutoSpeed.skip.after.adjust");

        skipAfterAdjustment = new BooleanParameter(dhtGroup, SpeedManagerAlgorithmProviderV2.SETTING_WAIT_AFTER_ADJUST);
        gridData = new GridData();
        gridData.widthHint = 50;
        skipAfterAdjustment.setLayoutData(gridData);

        //spacer
        cSpacer = new Label(cSection, SWT.NULL);
        gridData = new GridData();
        gridData.horizontalSpan=3;
        cSpacer.setLayoutData(gridData);

        return cSection;
    }


    void enableGroups(String strategyListValue){
        if(strategyListValue==null){
            return;
        }

        //only enable the comment section if the beta is enabled.
        boolean isBothEnabled = COConfigurationManager.getBooleanParameter( TransferSpeedValidator.AUTO_UPLOAD_ENABLED_CONFIGKEY );
        boolean isSeedingEnabled = COConfigurationManager.getBooleanParameter( TransferSpeedValidator.AUTO_UPLOAD_SEEDING_ENABLED_CONFIGKEY );
        long version = COConfigurationManager.getLongParameter( SpeedManagerImpl.CONFIG_VERSION );

        boolean isV2Enabled = false;
        if( (isBothEnabled || isSeedingEnabled) && version==2 ){
            isV2Enabled = true;
        }

        if( commentGroup!=null){
            if( isV2Enabled ){
                //make this section visible.
                commentGroup.setEnabled(true);
                commentGroup.setVisible(true);
            }else{
                //make it invisible.
                commentGroup.setEnabled(false);
                commentGroup.setVisible(false);
            }
        }
    }//enableGroups

    /**
     * Listen for changes in the drop down, then enable/disable the appropriate
     * group mode.
     */
    class GroupModeChangeListener implements ParameterChangeListener
    {
        /**
         * Enable/Disable approriate group.
         * @param p -
         * @param caused_internally  -
         */
        public void parameterChanged(Parameter p, boolean caused_internally) {

        }


        public void intParameterChanging(Parameter p, int toValue) {
            //nothing to do here.
        }


        public void booleanParameterChanging(Parameter p, boolean toValue) {
            //nothing to do here.
        }


        public void stringParameterChanging(Parameter p, String toValue) {
            //nothing to do here.
        }


        public void floatParameterChanging(Parameter owner, double toValue) {
            //nothing to do here.
        }
    }//class GroupModeChangeListener


    class RestoreDefaultsListener implements Listener {

        public void handleEvent(Event event) {

            ConfigurationDefaults configDefs = ConfigurationDefaults.getInstance();

            try{
                long downMax = configDefs.getLongParameter( SpeedManagerAlgorithmProviderV2.SETTING_DOWNLOAD_MAX_LIMIT);
                String downConf = configDefs.getStringParameter( SpeedLimitMonitor.DOWNLOAD_CONF_LIMIT_SETTING );
                long upMax = configDefs.getLongParameter( SpeedManagerAlgorithmProviderV2.SETTING_UPLOAD_MAX_LIMIT );
                String upConf = configDefs.getStringParameter( SpeedLimitMonitor.UPLOAD_CONF_LIMIT_SETTING );

                COConfigurationManager.setParameter(SpeedManagerAlgorithmProviderV2.SETTING_DOWNLOAD_MAX_LIMIT,downMax);
                COConfigurationManager.setParameter(SpeedLimitMonitor.DOWNLOAD_CONF_LIMIT_SETTING,downConf);
                COConfigurationManager.setParameter(SpeedManagerAlgorithmProviderV2.SETTING_UPLOAD_MAX_LIMIT,upMax);
                COConfigurationManager.setParameter(SpeedLimitMonitor.UPLOAD_CONF_LIMIT_SETTING,upConf); 

            }catch(ConfigurationParameterNotFoundException cpnfe){
                //ToDo: log this.    
            }


        }//handleEvent

    }//class

}
