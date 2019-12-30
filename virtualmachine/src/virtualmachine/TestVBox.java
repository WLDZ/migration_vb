package virtualmachine;
/* $Id: TestVBox.java 86321 2013-06-10 16:31:35Z klaus $ */

/* Small sample/testcase which demonstrates that the same source code can
 * be used to connect to the webservice and (XP)COM APIs. */

/*
 * Copyright (C) 2010-2013 Oracle Corporation
 *
 * This file is part of VirtualBox Open Source Edition (OSE), as
 * available from http://www.virtualbox.org. This file is free software;
 * you can redistribute it and/or modify it under the terms of the GNU
 * General Public License (GPL) as published by the Free Software
 * Foundation, in version 2 as it comes in the "COPYING" file of the
 * VirtualBox OSE distribution. VirtualBox OSE is distributed in the
 * hope that it will be useful, but WITHOUT ANY WARRANTY of any kind.
 */
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import javax.swing.text.MaskFormatter;

import org.virtualbox_4_3.CPUPropertyType;
import org.virtualbox_4_3.CloneMode;
import org.virtualbox_4_3.CloneOptions;
import org.virtualbox_4_3.ExportOptions;
import org.virtualbox_4_3.HWVirtExPropertyType;
import org.virtualbox_4_3.IAppliance;
import org.virtualbox_4_3.IConsole;
import org.virtualbox_4_3.IEvent;
import org.virtualbox_4_3.IEventListener;
import org.virtualbox_4_3.IEventSource;
import org.virtualbox_4_3.IGuestOSType;
import org.virtualbox_4_3.IMachine;
import org.virtualbox_4_3.IMachineDebugger;
import org.virtualbox_4_3.IMachineStateChangedEvent;
import org.virtualbox_4_3.IProgress;
import org.virtualbox_4_3.ISession;
import org.virtualbox_4_3.ISnapshot;
import org.virtualbox_4_3.IVirtualBox;
import org.virtualbox_4_3.IVirtualBoxErrorInfo;
import org.virtualbox_4_3.ImportOptions;
import org.virtualbox_4_3.LockType;
import org.virtualbox_4_3.VBoxEventType;
import org.virtualbox_4_3.VBoxException;
import org.virtualbox_4_3.VirtualBoxManager;


public class TestVBox
{
    static void processEvent(IEvent ev)
    {
        System.out.println("got event: " + ev);
        VBoxEventType type = ev.getType();
        System.out.println("type = " + type);
        switch (type)
        {
            case OnMachineStateChanged:
            {
                IMachineStateChangedEvent mcse = IMachineStateChangedEvent.queryInterface(ev);
                if (mcse == null)
                    System.out.println("Cannot query an interface");
                else
                    System.out.println("mid=" + mcse.getMachineId());
                break;
            }
        }
    }

    static class EventHandler
    {
        EventHandler() {}
        public void handleEvent(IEvent ev)
        {
            try {
                processEvent(ev);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    static void testEvents(VirtualBoxManager mgr, IEventSource es)
    {
        // active mode for Java doesn't fully work yet, and using passive
        // is more portable (the only mode for MSCOM and WS) and thus generally
        // recommended
        IEventListener listener = es.createListener();

        es.registerListener(listener, Arrays.asList(VBoxEventType.Any), false);

        try {
            for (int i = 0; i < 50; i++)
            {
                System.out.print(".");
                IEvent ev = es.getEvent(listener, 500);
                if (ev != null)
                {
                    processEvent(ev);
                    es.eventProcessed(listener, ev);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        es.unregisterListener(listener);
    }

    static void testEnumeration(VirtualBoxManager mgr, IVirtualBox vbox)
    {
        List<IMachine> machs = vbox.getMachines();
        for (IMachine m : machs)
        {
            String name;
            Long ram = 0L;
            boolean hwvirtEnabled = false, hwvirtNestedPaging = false;
            boolean paeEnabled = false;
            boolean inaccessible = false;
            try
            {
                name = m.getName();
                ram = m.getMemorySize();
                hwvirtEnabled = m.getHWVirtExProperty(HWVirtExPropertyType.Enabled);
                hwvirtNestedPaging = m.getHWVirtExProperty(HWVirtExPropertyType.NestedPaging);
                paeEnabled = m.getCPUProperty(CPUPropertyType.PAE);
                String osType = m.getOSTypeId();
                IGuestOSType foo = vbox.getGuestOSType(osType);
            }
            catch (VBoxException e)
            {
                name = "<inaccessible>";
                inaccessible = true;
            }
            System.out.println("VM name: " + name);
            if (!inaccessible)
            {
                System.out.println(" RAM size: " + ram + "MB"
                                   + ", HWVirt: " + hwvirtEnabled
                                   + ", Nested Paging: " + hwvirtNestedPaging
                                   + ", PAE: " + paeEnabled);
            }
        }
    }

    static boolean progressBar(VirtualBoxManager mgr, IProgress p, long waitMillis)
    {
        long end = System.currentTimeMillis() + waitMillis;
        while (!p.getCompleted())
        {
            mgr.waitForEvents(0);
            p.waitForCompletion(200);
            if (System.currentTimeMillis() >= end)
                return false;
        }
        return true;
    }
    /**
     *This function invokes the instance of virtualbox hypervisor on host operating system. Once the hypervisor is successfully started, 
     * a session VM registered inside the hypervisor starts to execute itself.
     * @param mgr
     * @param vbox
     * @param no Index of Virtual machine registered on the virtualbox hypervisor
     */

    static void testStart(VirtualBoxManager mgr, IVirtualBox vbox, int no)
    {
        IMachine m = vbox.getMachines().get(no);
        String name = m.getName();
        System.out.println("\nAttempting to start VM '" + name + "'");
        
        ISession session = mgr.getSessionObject();
        IProgress p = m.launchVMProcess(session, "gui", "");
        progressBar(mgr, p, 10000);
        session.unlockMachine();
    }



    static void testReadLog(VirtualBoxManager mgr, IVirtualBox vbox)
    {
        IMachine m =  vbox.getMachines().get(0);
        long logNo = 0;
        long off = 0;
        long size = 16 * 1024;
        while (true)
        {
            byte[] buf = m.readLog(logNo, off, size);
            if (buf.length == 0)
                break;
            System.out.print(new String(buf));
            off += buf.length;
        }
    }

    static void printErrorInfo(VBoxException e)
    {
        System.out.println("VBox error: " + e.getMessage());
        System.out.println("Error cause message: " + e.getCause());
        System.out.println("Overall result code: " + Integer.toHexString(e.getResultCode()));
        int i = 1;
        for (IVirtualBoxErrorInfo ei = e.getVirtualBoxErrorInfo(); ei != null; ei = ei.getNext(), i++)
        {
            System.out.println("Detail information #" + i);
            System.out.println("Error mesage: " + ei.getText());
            System.out.println("Result code:  " + Integer.toHexString(ei.getResultCode()));
            // optional, usually provides little additional information:
            System.out.println("Component:    " + ei.getComponent());
            System.out.println("Interface ID: " + ei.getInterfaceID());
        }
    }
   
 /** The main role of this function is clone a virtual machine. This function creates a full clone of the originally running VM. 
  *  Full clone means that a new copy of hard disks,configuration files and the other devices associated with the original virtual machine 
  *  will created
 
  * @param vbox
  * @param mgr
  * @param machine
  * @param path
  * @param name
  * @param vboxname
  *
  *
  */


    public static void cloneVM ( IVirtualBox vbox, VirtualBoxManager mgr ,int machine, String path , String name , String vboxname)
    
    {
        IMachine m = vbox.getMachines().get(machine);
        String ostype = m.getOSTypeId();
        ISession iSession1 = mgr.getSessionObject();
        vbox.getMachines().get(machine).lockMachine(iSession1, LockType.Shared);
        IMachine mac = vbox.createMachine(vbox.composeMachineFilename(vboxname,null,null,path),name, null, ostype,"forceOverwrite=1");
        mac.saveSettings();
        
        ArrayList <CloneOptions> options =new ArrayList<CloneOptions>();
        options.add(CloneOptions.KeepAllMACs);
        
        IProgress progr =  m.cloneTo(mac, CloneMode.MachineState,options);
        System.out.println("cloning");        
        progressBar(mgr, progr, 10000);
        vbox.registerMachine(mac);
        System.out.println("done");
    }

  //--------------------------------------------------------------------------------------------------------------------------       
   
  /**
   * t It displays the names as well as the indexes of the virtual machines registered on the hypervisor.
   *  It should be mentioned here that for successful execution of this function, 
   *  hypervisor should already be running otherwise this function will terminate
   *   
   *   
   * @param vbox
   */
   public static void getMachineNames(IVirtualBox vbox)
   {
	
	   List<IMachine> machs = vbox.getMachines();
	    
	    for (int i=0;i<machs.size();i++)
	    {
	    	try 
	    	{
	    		System.out.println("Index       Machine Name   ");
	    		System.out.println("=========================================");
	    		System.out.println(i+"              "+machs.get(i).getName());
	        } 
	    	catch (VBoxException e) 
	    	{
	            System.out.println(e.getMessage());
	            System.out.println(e.getVirtualBoxErrorInfo().getText());
	    	}     
	             	
	    }
		System.out.println("=========================================");
   }
    
   
   /**
    * The primary purpose of this function is to power off the virtual machine running inside 
    * the hypervisor while storing its running execution state
    * 
    * @param mgr
    * @param vbox
    * @param machineno 
    */
   

 //---------------------------------For Hibernating the VM---------------------------------------------
   
   public static void hibernate (VirtualBoxManager mgr , IVirtualBox vbox, int machineno)
   
   {
    	ISession iSession = mgr.getSessionObject();   
        vbox.getMachines().get(machineno).lockMachine(iSession, LockType.Shared);
        IConsole iConsole = iSession.getConsole();
        iConsole.saveState();
     
        try
        {
			int ch = System.in.read();
			System.out.println("press enter to continue");
		} 
        catch (IOException e) 
        {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    }
   
   /**
    * The purpose of this function to export the virtual machine to another directory in the form of .ovf extension. 
    * Exporting a machine to other directory in the form ovf appliance is basically done by this method
    * 
    * @param vbox
    * @param path
    * @param machineNo
    * @param ovfName
    */
    
    public static void exportOVF (IVirtualBox vbox, String path, int machineNo,String ovfName)
    {
        IMachine m = vbox.getMachines().get(machineNo);
        IAppliance appl = vbox.createAppliance(); 
        m.exportTo(appl, path);
        
        List<ExportOptions> options =new ArrayList<ExportOptions>();
        options.add(ExportOptions.CreateManifest);
        
        IProgress prog1 =  appl.write("ovf-2.0",options, path+"/"+ovfName+".ovf");
        System.out.println("Please wait while VM is being exported");
        prog1.waitForCompletion(-1);
    }
    
    /**
     * The main purpose of this machine is to import an ovf appliance in to the virtualbox hypervisor. 
     * Once an appliance is successfully imported in to the hypervisor, its execution can be started.
     * 
     * @param vbox
     * @param path
     */
    
    public static void importOVF (IVirtualBox vbox, String path)
    {
        IAppliance appliance = vbox.createAppliance();
        IProgress  progress = appliance.read(path);
        appliance.interpret();
        progress.waitForCompletion(-1);
        
        List<ImportOptions> options =new ArrayList<ImportOptions>();
        options.add(ImportOptions.KeepAllMACs);
        
        IProgress prog =  appliance.importMachines(options);
        System.out.println("Please wait VM is being imported ");
        prog.waitForCompletion(-1);
        System.out.println("Done");
    }
    
    /**
     * he primary job of this function is to get the core dump of a virtual machine running inside a virtualbox hypervisor. 
     * Core dump or memory dump is the record about the collection of memory states running programs at given interval of time.
     *  Once this method is executed successfully,a core dump is generated in the form of .sav extension in the specified directory
     * 
     * @param vbox
     * @param mgr
     * @param path
     * @param dumpName
     * @param machineNo
     */
    
    
    public static void dump(IVirtualBox vbox, VirtualBoxManager mgr,String path, String dumpName,int machineNo)
    {
    	ISession iSession = mgr.getSessionObject();
        vbox.getMachines().get(machineNo).lockMachine(iSession, LockType.Shared);
        IConsole iConsole = iSession.getConsole();
        IMachineDebugger dump = iConsole.getDebugger();
        dump.dumpGuestCore(path+"/"+dumpName,"");
    }
    
    /**
     * The main role of this function is to register a virtual machine on the hypervisor that is not already present on the hypervisor. 
     * All that this needs is the full path of the directory where the files associated with the VM are located and the name of the virtual machine 
     * in order to register the machine on the hypervisor.
     * 
     * @param vbox
     * @param mgr
     * @param path
     * @param vmName
     */
    
    public static void registerMachine(IVirtualBox vbox, VirtualBoxManager mgr,String path,String vmName)
    {
    	ISession session = mgr.getSessionObject();
    	IMachine mac = vbox.openMachine(path+"/"+vmName);
    	vbox.registerMachine(mac);
    }
    
    /**
     * The main purpose of this method is to create capture the snapshot of the memory of a particular VM running inside the hypervisor.
     * 
     * @param vbox
     * @param mgr
     * @param machineNo
     * @param snapshotName
     */
    
    public static void captureSnapshot(IVirtualBox vbox, VirtualBoxManager mgr,int machineNo,String snapshotName)
    {
        ISession iSession = mgr.getSessionObject();
        vbox.getMachines().get(machineNo).lockMachine(iSession, LockType.Shared);
        IConsole iConsole = iSession.getConsole();
        IProgress prog = iConsole.takeSnapshot(snapshotName, "");
        prog.waitForCompletion(-1);
    }
    
    /**
     * The primary job of this function is to reset the VM to point of the time at which the memory snapshot has been taken. 
     * In short, VM starts its execution from state of memory at which snapshot has been captured.
     * @param snapshotName
     * @param machineName
     * @param machineNo
     * @param vbox
     * @param mgr
     */
    
    
    public static void restoreSnapshort(String snapshotName,String machineName,int machineNo, IVirtualBox vbox, VirtualBoxManager mgr )
    {
    	
        ISnapshot snap =   vbox.getMachines().get(machineNo).findSnapshot(snapshotName);
        ISession iSession = mgr.getSessionObject();
        vbox.getMachines().get(machineNo).lockMachine(iSession, LockType.Shared);
        IConsole iConsole = iSession.getConsole();
        IProgress prog = iConsole.restoreSnapshot(snap);
        prog.waitForCompletion(-1);
        
        IMachine mac = vbox.findMachine(machineName);
     	iSession.unlockMachine();
     	testStart(mgr, vbox,machineNo);
    	
    }
    

    
    

    public static void main(String[] args)
    {
        VirtualBoxManager mgr = VirtualBoxManager.createInstance(null);
        IVirtualBox vbox = mgr.getVBox();
        boolean ws = false;
        String  url = null;
        String  user = null;
        String  passwd = null;

        for (int i = 0; i < args.length; i++)
        {
            if (args[i].equals("-w"))
                ws = true;
            else if (args[i].equals("-url"))
                url = args[++i];
            else if (args[i].equals("-user"))
                user = args[++i];
            else if (args[i].equals("-passwd"))
                passwd = args[++i];
        }

        if (ws)
        {
            try {
                mgr.connect(url, user, passwd);
            } catch (VBoxException e) {
                e.printStackTrace();
                System.out.println("Cannot connect, start webserver first!");
            }
        }

        try
        {
            
            if (vbox != null)
            {
               System.out.println("VirtualBox version: " + vbox.getVersion() + "\n");



            
            System.out.println("done, press Enter...");
                int ch = System.in.read();
                
                
            }
        }
        catch (VBoxException e)
        {
            printErrorInfo(e);
            System.out.println("Java stack trace:");
            e.printStackTrace();
        }
        catch (RuntimeException e)
        {
            System.out.println("Runtime error: " + e.getMessage());
            e.printStackTrace();
        }
        catch (java.io.IOException e)
        {
            e.printStackTrace();
        }

        if (ws)
        {
            try {
                mgr.disconnect();
            } catch (VBoxException e) {
                e.printStackTrace();
            }
        }
       
        try {
			int ch = System.in.read();
			System.out.println("press enter to continue");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        

      

while(true)
{
        
        try
        {
        	System.out.println("Virtual Machines Present At The Momnent");
        	getMachineNames(vbox);       

		} 
        catch (Exception e) 
        {
			System.out.println(e.getMessage());
		}
       
        
        
        int machineNo;
        String path;
        String snapshotName;
        
        System.out.println("Press S To Start The Virtual Machine");
        System.out.println("Press H To Hibernate The Virtual Machine");
        System.out.println("Press C To Clone The Virtual Machine");
        System.out.println("Press E To Export The Virtual Machine");
        System.out.println("Press I To Import The Virtual Machine");
        System.out.println("Press M To Migrate The Virtual Machine");
        System.out.println("Press D To Create The Meomory Dump The Virtual Machine");
        System.out.println("Press CS To Capture a Snapshot Of The Virtual Machine");
        System.out.println("Press RS To Capture a Snapshot Of The Virtual Machine");
        Scanner in = new Scanner(System.in);
        
        
        
        String caseM = in.nextLine();
        
        if (caseM.equals("S"))
        {
        	System.out.println("Enter The Index of Virtual Machine Which You Want to Start"); 
        	machineNo = Integer.parseInt( in.nextLine());
        	testStart(mgr, vbox, machineNo);
        	System.out.println("Machine Has Been Successfully Started");
        	
        }
        
        else if (caseM.equals("H"))
        {
        	System.out.println("Enter The Index of Virtual Machine Which You Want to Hibernate");      
        	machineNo = Integer.parseInt( in.nextLine());
        	hibernate(mgr, vbox,machineNo); 
        	System.out.println("Machine Has Been Successfully Hibernated");
        	
        }
        
        else if (caseM.equals("C"))
        {
        	
        	System.out.println("Enter The Index of Virtual Machine To Clone The Virtual Machine To Other Destination");
        	machineNo = Integer.parseInt( in.nextLine());
        	System.out.println("Enter the Path of the directory Where You Want To Clone The VM");
        	path = in.nextLine();
        	System.out.println("Enter The Name Of The Folder Where You Want To Clone The VM");
        	String folderName = in.nextLine();
        	System.out.println("Enter The Name For The Cloned VM");
        	String vmName = in.nextLine();
        	cloneVM(vbox, mgr,machineNo, path, folderName, vmName);  
        	System.out.println("Machine Has Been Successfully Cloned");
        }
        
        else if (caseM.equals("E"))
        {
        	
        	System.out.println("Enter The Path Where VM Will Be Exprted In The Form Of OVF");
        	path = in.nextLine();
        	System.out.println("Enter The Name By Which VM Will Be Saved In OVF Exstenison ");
        	String ovfnName = in.nextLine();
        	System.out.println("Enter The Index of Virtual Machine To Export");
        	machineNo = Integer.parseInt( in.nextLine());
        	exportOVF(vbox,path,machineNo,ovfnName); 
        	System.out.println("Machine Has Been Successfully Exported");
        	
        }
        
        else if (caseM.equals("I"))
        {
        	System.out.println("Enter The Path From Where VM Will Be Imported");
        	path = in.nextLine();
        	importOVF(vbox, path); 
        	System.out.println("Machine Has Been Successfully Imported");
        	
        }
        
        
        
        
        
        else if (caseM.equals("D"))
        {
        	
        	System.out.println("Enter The Index of Virtual Machine Of Which You Want TO Take Memory Dump");
        	machineNo = Integer.parseInt( in.nextLine());
        	System.out.println("Enter the Path of the directory Where You Want Save The Memory Dump");
        	path = in.nextLine();
        	System.out.println("Enter The Name That Will Be Given To The Saved Memory Dump");
        	String dumpName = in.nextLine();
        	dump(vbox, mgr, path, dumpName, machineNo);
        	System.out.println("Memory Dump Is Generated Suceesfully ");
        	
        }
        

        else if (caseM.equals("CS"))
        {
        	
        	System.out.println("Enter The Index of Virtual Machine Of Which You Want TO Take Snapshot");
        	machineNo = Integer.parseInt( in.nextLine());
        	System.out.println("Enter Name For The Snapshot Which You Want To Create");
        	snapshotName = in.nextLine();
        	captureSnapshot(vbox, mgr, machineNo, snapshotName);
        	System.out.println("Snapshot Of The Memory Has Been Captured Sucessfully");
        }
        
        

        else if (caseM.equals("RS"))
        {
        	System.out.println("Enter The Index of Virtual Machine Of Which You Want TO Restore Snapshot");
        	machineNo = Integer.parseInt( in.nextLine());
        	System.out.println("Enter Name of The Snapshot Which You Want To Restore ");
        	snapshotName = in.nextLine();
        	System.out.println("Enter The Name of The VM");
        	String machineName = in.nextLine();
        	restoreSnapshort(snapshotName, machineName, machineNo, vbox, mgr);
        	System.out.println("Snapshot Of The Memory Has Been Restored Sucessfully");
        	
        }
        
        else if (caseM.equals("M") )
        {
        	System.out.println("Please Wait While Resources Are Being Selected To Accommodate the Migration");

        	boolean check = false;
				try
				{
					new Client(1);
					new Client(2);
					System.out.println("Resources Have Been Successfully Reservered");
					
					
					try 
					{
						Thread.sleep(1000);
					} catch (InterruptedException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					
					
					new Client(1);
					new Client(10);
					
					try 
					{
						Thread.sleep(1000);
					} catch (InterruptedException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					registerMachine(vbox, mgr, "", "Ubuntu1");
					
					
		        	System.out.println("Enter Name of The Snapshot Which You Want To Restore ");
		        	snapshotName = in.nextLine();
		        	restoreSnapshort(snapshotName,"Ubuntu1",0, vbox, mgr);
		        	System.out.println("Snapshot Of The Memory Has Been Restored Sucessfully");
					
//		            testStart(mgr, vbox,0);
		        	
		        	
				
					
				} 
				catch (IOException e) 
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
//			} 
        	
        }
        
        
        
        
        
        mgr.cleanup();

    }
    }

}