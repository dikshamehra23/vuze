/**
 * 
 */
package com.aelitis.azureus.ui.swt.skin;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.shell.LightBoxShell;

import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;

/**
 * @author TuxPaper
 * @created Jun 8, 2006
 *
 */
public class SWTSkinUtils
{

	public static final int TILE_NONE = 0;

	public static final int TILE_Y = 1;

	public static final int TILE_X = 2;

	public static final int TILE_CENTER_X = 4;

	public static final int TILE_CENTER_Y = 8;

	public static final int TILE_BOTH = TILE_X | TILE_Y;

	private static Listener imageDownListener;

	private static Listener imageOverListener;

	static {
		imageOverListener = new SWTSkinImageChanger("-over", SWT.MouseEnter,
				SWT.MouseExit);
		imageDownListener = new SWTSkinImageChanger("-down", SWT.MouseDown,
				SWT.MouseUp);
	}

	public static int getAlignment(String sAlign, int def) {
		int align;

		if (sAlign == null) {
			align = def;
		} else if (sAlign.equalsIgnoreCase("center")) {
			align = SWT.CENTER;
		} else if (sAlign.equalsIgnoreCase("bottom")) {
			align = SWT.BOTTOM;
		} else if (sAlign.equalsIgnoreCase("top")) {
			align = SWT.TOP;
		} else if (sAlign.equalsIgnoreCase("left")) {
			align = SWT.LEFT;
		} else if (sAlign.equalsIgnoreCase("right")) {
			align = SWT.RIGHT;
		} else {
			align = def;
		}

		return align;
	}

	/**
	 * @param tileMode
	 * @return
	 */
	public static int getTileMode(String sTileMode) {
		int tileMode = TILE_NONE;
		if (sTileMode == null || sTileMode == "") {
			return tileMode;
		}

		sTileMode = sTileMode.toLowerCase();

		if (sTileMode.equals("tile")) {
			tileMode = TILE_X | TILE_Y;
		} else if (sTileMode.equals("tile-x")) {
			tileMode = TILE_X;
		} else if (sTileMode.equals("tile-y")) {
			tileMode = TILE_Y;
		} else if (sTileMode.equals("center-x")) {
			tileMode = TILE_CENTER_X;
		} else if (sTileMode.equals("center-y")) {
			tileMode = TILE_CENTER_Y;
		}

		return tileMode;
	}

	static void addMouseImageChangeListeners(Widget widget) {
		if (widget.getData("hasMICL") != null) {
			return;
		}

		widget.addListener(SWT.MouseEnter, imageOverListener);
		widget.addListener(SWT.MouseExit, imageOverListener);
		//		new MouseEnterExitListener(widget);

		widget.addListener(SWT.MouseDown, imageDownListener);
		widget.addListener(SWT.MouseUp, imageDownListener);

		widget.setData("hasMICL", "1");
	}

	public static void setVisibility(SWTSkin skin, String configID,
			String viewID, boolean visible) {
		setVisibility(skin, configID, viewID, visible, true, false);
	}

	public static void setVisibility(SWTSkin skin, String configID,
			String viewID, final boolean visible, boolean save, boolean fast) {

		SWTSkinObject skinObject = skin.getSkinObject(viewID);
		
		if (skinObject == null) {
			Debug.out("setVisibility on non existing skin object: " + viewID);
			return;
		}
		
		if (skinObject.isVisible() == visible && skin.getShell().isVisible()) {
			return;
		}

		final Control control = skinObject.getControl();

		if (control != null && !control.isDisposed()) {
			Point size;
			if (visible) {
				final FormData fd = (FormData) control.getLayoutData();
				size = (Point) control.getData("v3.oldHeight");
				//System.out.println(control.getData("SkinID") + " oldHeight = " + size + ";v=" + control.getVisible() + ";s=" + control.getSize());
				if (size == null) {
					size = control.computeSize(SWT.DEFAULT, SWT.DEFAULT);
					if (fd.height > 0) {
						size.y = fd.height;
					}
					if (fd.width > 0) {
						size.x = fd.width;
					}
				}
			} else {
				size = new Point(0, 0);
			}
			setVisibility(skin, configID, skinObject, size, save, fast, null);
		}
	}

	public static void setVisibility(SWTSkin skin, String configID,
			final SWTSkinObject skinObject, final Point destSize, boolean save,
			boolean fast, Runnable runAfterSlide) {
		boolean visible = destSize.x != 0 || destSize.y != 0;
		try {
			if (skinObject == null) {
				return;
			}
			final Control control = skinObject.getControl();
			if (control != null && !control.isDisposed()) {
				if (visible) {
					FormData fd = (FormData) control.getLayoutData();
					fd.width = 0;
					fd.height = 0;
					control.setData("oldSize", new Point(0, 0));

					skinObject.setVisible(visible);

					// FormData should now be 0,0, but setVisible may have 
					// explicitly changed it
					fd = (FormData) control.getLayoutData();
					
					if (fd.width != 0 || fd.height != 0) {
						return;
					}

					if (destSize != null) {
						if (fd != null
								&& (fd.width != destSize.x || fd.height != destSize.y)) {
							if (fast) {
								fd.width = destSize.x;
								fd.height = destSize.y;
								control.setLayoutData(fd);
								Utils.relayout(control);
							} else {
								slide(skinObject, fd, destSize, runAfterSlide);
								runAfterSlide = null; // prevent calling again
							}
						}
					} else {
						if (fd.width == 0) {
							fd.width = SWT.DEFAULT;
						}
						if (fd.height == 0) {
							fd.height = SWT.DEFAULT;
						}
						control.setLayoutData(fd);
						Utils.relayout(control);
					}
					control.setData("v3.oldHeight", null);
				} else {
					final FormData fd = (FormData) control.getLayoutData();
					if (fd != null) {
						Point oldSize = new Point(fd.width, fd.height);
						if (oldSize.y <= 0) {
							oldSize = null;
						}
						control.setData("v3.oldHeight", oldSize);

						if (fast) {
							skinObject.setVisible(false);
						} else {
							slide(skinObject, fd, destSize, runAfterSlide);
							runAfterSlide = null; // prevent calling again
						}
					}
				}
			}

			if (save
					&& COConfigurationManager.getBooleanParameter(configID) != visible) {
				COConfigurationManager.setParameter(configID, visible);
			}
		} finally {
			if (runAfterSlide != null) {
				runAfterSlide.run();
			}
		}
	}

	public static void fade(final Composite c, final boolean fadeIn) {
		UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
		final LightBoxShell lbShell = new LightBoxShell(uiFunctions.getMainShell(),
				false);

		// assumed: c is on shell
		Rectangle clientArea = c.getClientArea();
		lbShell.setInsets(0, c.getShell().getClientArea().height
				- clientArea.height, 0, 0);
		lbShell.setStyleMask(LightBoxShell.RESIZE_HORIZONTAL);
		lbShell.setAlphaLevel(fadeIn ? 255 : 0);
		lbShell.open();
		AERunnable runnable = new AERunnable() {
			public void runSupport() {
				if (c.isDisposed()) {
					return;
				}

				int alphaLevel = lbShell.getAlphaLevel();
				if (fadeIn) {
					alphaLevel -= 5;
					if (alphaLevel < 0) {
						lbShell.close();
						return;
					}
				} else {
					alphaLevel += 5;
					if (alphaLevel > 255) {
						lbShell.close();
						return;
					}
				}
				lbShell.setAlphaLevel(alphaLevel);

				final AERunnable r = this;
				SimpleTimer.addEvent("fade", SystemTime.getCurrentTime() + 10,
						new TimerEventPerformer() {
							public void perform(TimerEvent event) {
								c.getDisplay().asyncExec(r);
							}
						});
			}
		};

		c.getDisplay().asyncExec(runnable);
	}

	public static void slide(final SWTSkinObject skinObject, final FormData fd,
			final Point destSize, final Runnable runOnCompletion) {
		final Control control = skinObject.getControl();
		//System.out.println("slide to " + size + " via "+ Debug.getCompressedStackTrace());
		boolean exit = Utils.execSWTThreadWithBool("slide",
				new AERunnableBoolean() {
					public boolean runSupport() {
						boolean exit = control.getData("slide.active") != null;
						Runnable oldROC = (Runnable) control.getData("slide.runOnCompletion");
						if (oldROC != null) {
							oldROC.run();
						}
						control.setData("slide.destSize", destSize);
						control.setData("slide.runOnCompletion", runOnCompletion);
						if (destSize.y > 0) {
							skinObject.setVisible(true);
						}
						return exit;
					}
				}, 1000);

		if (exit) {
			return;
		}

		AERunnable runnable = new AERunnable() {
			boolean firstTime = true;

			float pct = 0.4f;

			public void runSupport() {
				if (control.isDisposed()) {
					return;
				}
				Point size = (Point) control.getData("slide.destSize");
				if (size == null) {
					return;
				}

				if (firstTime) {
					firstTime = false;
					control.setData("slide.active", "1");
				}

				int newWidth = (int) (fd.width + (size.x - fd.width) * pct);
				int h = fd.height >= 0 ? fd.height : control.getSize().y;
				int newHeight = (int) (h + (size.y - h) * pct);
				pct += 0.01;
				//System.out.println(control + "] newh=" + newHeight + "/" + newWidth + " to " + size.y);

				if (newWidth == fd.width && newHeight == h) {
					fd.width = size.x;
					fd.height = size.y;
					//System.out.println(control + "] side to " + size.y + " done" + size.x);
					control.setLayoutData(fd);
					Utils.relayout(control);
					control.getParent().layout();

					control.setData("slide.active", null);
					control.setData("slide.destSize", null);
					
					if (newHeight == 0) {
						skinObject.setVisible(false);
						Utils.relayout(control);
					}

					Runnable oldROC = (Runnable) control.getData("slide.runOnCompletion");
					if (oldROC != null) {
						control.setData("slide.runOnCompletion", null);
						oldROC.run();
					}
				} else {
					fd.width = newWidth;
					fd.height = newHeight;
					control.setLayoutData(fd);
					//Utils.relayout(control, false);
					control.getParent().layout();

					Utils.execSWTThreadLater(20, this);
				}
			}
		};
		control.getDisplay().asyncExec(runnable);
	}

	public static class MouseEnterExitListener
		implements Listener
	{

		boolean bOver = false;

		public MouseEnterExitListener(Widget widget) {

			widget.addListener(SWT.MouseMove, this);
			widget.addListener(SWT.MouseExit, this);
		}

		public void handleEvent(Event event) {
			Control control = (Control) event.widget;

			SWTSkinObject skinObject = (SWTSkinObject) control.getData("SkinObject");

			if (event.type == SWT.MouseMove) {
				if (bOver) {
					return;
				}
				System.out.println(System.currentTimeMillis() + ": " + skinObject
						+ "-- OVER");
				bOver = true;
				skinObject.switchSuffix("-over", 2, true);

			} else {
				bOver = false;
				skinObject.switchSuffix("", 2, true);
			}

		}

	}
}
